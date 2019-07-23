package eu.openanalytics.phaedra.project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import com.google.common.base.Objects;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.SecurityService.Action;
import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.project.vo.Project;

public class ProjectService extends BaseJPAService {

	private static ProjectService instance = new ProjectService();
	
	public static ProjectService getInstance() {
		return instance;
	}


	private ProjectService() {
	}

	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}


	public List<Project> getReadableProjects() {
		return getProjects()
				.filter((project) -> SecurityService.getInstance().checkPersonalObject(Action.READ, project))
				.collect(Collectors.toList());
	}
	
	public List<Project> getWritableProjects() {
		return getProjects()
				.filter((project) -> SecurityService.getInstance().checkPersonalObject(Action.UPDATE, project))
				.collect(Collectors.toList());
	}

	public List<Project> getPrivateProjects() {
		String currentUser = SecurityService.getInstance().getCurrentUserName();
		return getProjects()
				.filter((project) -> project.getAccessScope().isPrivateScope())
				.filter((project) -> project.getOwner().equalsIgnoreCase(currentUser))
				.collect(Collectors.toList());
	}
	
	public List<Project> getTeamProjects() {
		return getProjects()
				.filter((project) -> project.getAccessScope().isTeamScope())
				.filter((project) -> SecurityService.getInstance().checkPersonalObject(Action.READ, project))
				.collect(Collectors.toList());
	}
	
	public List<Project> getPublicProjects() {
		return getProjects()
				.filter((project) -> project.getAccessScope().isPublicScope())
				.collect(Collectors.toList());
	}

	private Stream<Project> getProjects() {
		return streamableList(getList(Project.class)).stream();
	}


	public Project createProject(AccessScope accessScope) {
		if (accessScope == null) accessScope = AccessScope.PRIVATE;
		
		Project project = new Project();
		project.setName("New Project");
		project.setDescription("");
		project.setOwner(SecurityService.getInstance().getCurrentUserName());
		project.setTeamCode("NONE");
		project.setAccessScope(accessScope);
		
		return project;
	}
	
	public void updateProject(Project project) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, project);
		
		save(project);
	}
	
	public Project getWorkingCopy(Project project) {
		Project workingCopy = new Project();
		workingCopy.setId(project.getId());
		copy(project, workingCopy);
		
		return workingCopy;
	}
	
	public void updateProject(Project project, Project workingCopy) {
		if (workingCopy.getId() != project.getId()) throw new IllegalArgumentException();
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, project);
		if (!Objects.equal(workingCopy.getOwner(), project.getOwner())
				|| !Objects.equal(workingCopy.getAccessScope(), project.getAccessScope())
				|| !Objects.equal(workingCopy.getTeamCode(), project.getTeamCode())) {
			SecurityService.getInstance().checkPersonalObjectWithException(Action.DELETE, project);
		}
		copy(workingCopy, project);
		
		save(project);
	}
	
	public void deleteProject(Project project) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.DELETE, project);
		
		delete(project);
	}

	private void copy(Project from, Project to) {
		to.setName(from.getName());
		to.setDescription(from.getDescription());
		to.setOwner(from.getOwner());
		to.setTeamCode(from.getTeamCode());
		to.setAccessScope(from.getAccessScope());
	}


	public List<Experiment> getExperiments(Project project) {
		EntityManager em = getEntityManager();
		PlateService plateService = PlateService.getInstance();
		PreparedStatement selectExp = null;
		JDBCUtils.lockEntityManager(em);
		List<Long> ids;
		try (Connection connection = Screening.getEnvironment().getJDBCConnection()) {
			ids = getExperimentIds(project, connection);
		} catch (SQLException e) {
			EclipseLog.error(e.getMessage(), e, Activator.PLUGIN_ID);
			return Collections.emptyList();
		} finally {
			JDBCUtils.unlockEntityManager(em);	
			if (selectExp != null) try { selectExp.close(); } catch (SQLException e) {};
		}
		
		return ids.stream()
				.map((id) -> plateService.getExperiment(id))
				.filter((experiment) -> (experiment != null))
				.collect(Collectors.toList());
	}
	
	public List<Experiment> addExperiments(Project project, List<Experiment> experiments) {
		EntityManager em = getEntityManager();
		PreparedStatement insertExperiment = null;
		JDBCUtils.lockEntityManager(em);
		try (Connection connection = Screening.getEnvironment().getJDBCConnection()) {
			List<Experiment> added = new ArrayList<>(experiments.size());
			List<Long> existingIds = getExperimentIds(project, connection);
			insertExperiment = connection.prepareStatement(
					"insert into phaedra.hca_project_experiment(PROJECT_ID, EXPERIMENT_ID)"
					+ " values (?, ?)");
			for (Experiment experiment : experiments) {
				if (!existingIds.contains(experiment.getId())) {
					insertExperiment.setLong(1, project.getId());
					insertExperiment.setLong(2, experiment.getId());
					insertExperiment.addBatch();
					added.add(experiment);
				}
			}
			insertExperiment.executeBatch();
			connection.commit();
			
			fire(ModelEventType.ObjectChanged, project, 0);
			return added;
		} catch (SQLException e) {
			EclipseLog.error(e.getMessage(), e, Activator.PLUGIN_ID);
			throw new PersistenceException(e);
		} finally {
			JDBCUtils.unlockEntityManager(em);	
			if (insertExperiment != null) try { insertExperiment.close(); } catch (SQLException e) {};
		}
	}
	
	public void removeExperiments(Project project, List<Experiment> experiments) {
		EntityManager em = getEntityManager();
		PreparedStatement deleteExperiment = null;
		JDBCUtils.lockEntityManager(em);
		try (Connection connection = Screening.getEnvironment().getJDBCConnection()) {
			deleteExperiment = connection.prepareStatement(
					"delete from phaedra.hca_project_experiment"
					+ " where PROJECT_ID = ? and EXPERIMENT_ID = ?");
			for (Experiment experiment : experiments) {
				deleteExperiment.setLong(1, project.getId());
				deleteExperiment.setLong(2, experiment.getId());
				deleteExperiment.addBatch();
			}
			deleteExperiment.executeBatch();
			connection.commit();
			
			fire(ModelEventType.ObjectChanged, project, 0);
		} catch (SQLException e) {
			EclipseLog.error(e.getMessage(), e, Activator.PLUGIN_ID);
			throw new PersistenceException(e);
		} finally {
			JDBCUtils.unlockEntityManager(em);	
			if (deleteExperiment != null) try { deleteExperiment.close(); } catch (SQLException e) {};
		}
	}

	private List<Long> getExperimentIds(Project project, Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
					"select EXPERIMENT_ID"
					+ " from phaedra.hca_project_experiment"
					+ " where PROJECT_ID = ?")) {
			statement.setLong(1, project.getId());
			ResultSet resultSet = statement.executeQuery();
			List<Long> ids = new ArrayList<>();
			while (resultSet.next()) {
				ids.add(resultSet.getLong(1));
			}
			return ids;
		}
	}


	protected void fire(ModelEventType type, Object object, int status) {
		ModelEvent event = new ModelEvent(object, type, status);
		ModelEventService.getInstance().fireEvent(event);
	}

	@Override
	protected void afterSave(Object o) {
		fire(ModelEventType.ObjectChanged, o, 0);
	}
	
	@Override
	protected void beforeDelete(Object o) {
		fire(ModelEventType.ObjectAboutToBeRemoved, o, 0);
	}
	
	@Override
	protected void afterDelete(Object o) {
		fire(ModelEventType.ObjectRemoved, o, 0);
	}

}
