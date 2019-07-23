package eu.openanalytics.phaedra.ui.project.navigator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.NavigatorContentProvider;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.Group;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.project.ProjectService;
import eu.openanalytics.phaedra.project.vo.Project;

public class ProjectProvider implements IElementProvider {

	public static final String PROJECTS_ROOT_ID = "projects";
	
	public static final String PRIVATE_PROJECTS_ID = "projects/my";
	public static final String TEAM_PROJECTS_ID = "projects/team";
	public static final String PUBLIC_PROJECTS_ID = "projects/public";
	
	public static final String PROJECT_ID_PREFIX = "project-";
	public static final String EXPERIMENT_ID_PREFIX = "project/experiment-";


	private static String getScopeGroupId(AccessScope accessScope) {
		switch (accessScope) {
		case PRIVATE:
			return PRIVATE_PROJECTS_ID;
		case TEAM:
			return TEAM_PROJECTS_ID;
		default:
			return PUBLIC_PROJECTS_ID;
		}
	}
	
	private static String getElementId(Project project) {
		return PROJECT_ID_PREFIX + project.getId();
	}
	
	private static String getElementId(Experiment experiment) {
		return EXPERIMENT_ID_PREFIX + experiment.getId();
	}


	public static IElement[] getElementPath(Project project) {
		String scopeGroupId = getScopeGroupId(project.getAccessScope());
		IElement[] path = new IElement[] {
				new Group("", PROJECTS_ROOT_ID, NavigatorContentProvider.ROOT_GROUP.getId()),
				new Group("", scopeGroupId, PROJECTS_ROOT_ID),
				new Group("", getElementId(project), scopeGroupId),
		};
		for (int i = 1; i < path.length; i++) {
			((Element)path[i]).setParent((IGroup) path[i-1]);
		}
		return path;
	}
	
	public static List<IElement[]> getElementPaths(Project project, List<Experiment> experiments) {
		IElement[] projectPath = getElementPath(project);
		IGroup projectElement = (IGroup)projectPath[projectPath.length - 1];
		List<IElement[]> paths = new ArrayList<>(experiments.size());
		for (Experiment experiment : experiments) {
			Element expElement = new Element("", getElementId(experiment), projectElement.getId());
			expElement.setParent(projectElement);
			IElement[] path = Arrays.copyOf(projectPath, projectPath.length + 1);
			path[projectPath.length] = expElement;
			paths.add(path);
		}
		return paths;
	}


	public ProjectProvider() {
	}


	@Override
	public IElement[] getChildren(IGroup parent) {
		if (parent == NavigatorContentProvider.ROOT_GROUP) {
			return new IElement[] {
				new Group("Projects", PROJECTS_ROOT_ID, parent.getId(), true, null) };
		}
		else if (parent.getId().equals(PROJECTS_ROOT_ID)) {
			return new IElement[] {
				new Group("My Private Projects", PRIVATE_PROJECTS_ID, parent.getId(), IconManager.getIconDescriptor("user.png")),
				new Group("My Team Projects", TEAM_PROJECTS_ID, parent.getId(), IconManager.getIconDescriptor("team.png")),
				new Group("Public Projects", PUBLIC_PROJECTS_ID, parent.getId(), IconManager.getIconDescriptor("group.png")) };
		}
		else if (parent.getId().equals(PRIVATE_PROJECTS_ID)) {
			return getProjectElements(parent, ProjectService.getInstance().getPrivateProjects());
		}
		else if (parent.getId().equals(TEAM_PROJECTS_ID)) {
			return getProjectElements(parent, ProjectService.getInstance().getTeamProjects());
		}
		else if (parent.getId().equals(PUBLIC_PROJECTS_ID)) {
			return getProjectElements(parent, ProjectService.getInstance().getPublicProjects());
		}
		else if (parent.getId().startsWith(PROJECT_ID_PREFIX)) {
			Project project = (Project) parent.getData();
			List<Experiment> experiments = ProjectService.getInstance().getExperiments(project);
			IElement[] children = new IElement[experiments.size()];
			for (int i = 0; i < children.length; i++) {
				Experiment experiment = experiments.get(i);
				Element element = new Element(experiment.toString(), getElementId(experiment), parent.getId());
				element.setData(experiment);
				element.setImageDescriptor(IconManager.getDefaultIconDescriptor(experiment.getClass()));
				children[i] = element;
			}
			return children;
		}
		
		return null;
	}

	private IElement[] getProjectElements(IElement parent, List<Project> projects) {
		IElement[] children = new IElement[projects.size()];
		ImageDescriptor imageDescriptor = IconManager.getDefaultIconDescriptor(Project.class);
		for (int i = 0; i < children.length; i++) {
			Project project = projects.get(i);
			Group group = new Group(project.getName(), getElementId(project), parent.getId(), imageDescriptor);
			group.setData(project);
			children[i] = group;
		}
		return children;
	}

}
