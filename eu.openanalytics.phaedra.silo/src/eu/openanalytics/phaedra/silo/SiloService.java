package eu.openanalytics.phaedra.silo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.SecurityService.Action;
import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.accessor.SubWellSiloAccessor;
import eu.openanalytics.phaedra.silo.accessor.WellSiloAccessor;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloGroup;

public class SiloService extends BaseJPAService {

	private static SiloService instance;

	private Map<Silo, ISiloAccessor<? extends PlatformObject>> siloAccessors;

	private SiloService() {
		// Hidden constructor
		this.siloAccessors = new ConcurrentHashMap<>();
	}

	public static SiloService getInstance() {
		if (instance == null) instance = new SiloService();
		return instance;
	}

	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}

	/*
	 * **********
	 * Public API
	 * **********
	 */

	private List<Silo> getSilos() {
		return getList(Silo.class);
	}

	private List<Silo> getSilos(GroupType type) {
		String query = "select s from Silo s where s.type = ?1";
		return getList(query, Silo.class, type.getType());
	}

	public List<Silo> getMySilos(GroupType type) {
		String currentUser = SecurityService.getInstance().getCurrentUserName();
		String query = "select s from Silo s where s.owner = ?1 and s.type = ?2";
		return getList(query, Silo.class, currentUser, type.getType());
	}

	/**
	 * Get all the silos that are private and that do not belong to a SiloGroup.
	 * @param type
	 * @return
	 */
	public List<Silo> getPrivateSilos(GroupType type) {
		List<Silo> list = new ArrayList<>();
		List<Silo> tempList = getMySilos(type);
		for (Silo s : tempList) {
			if (s.getAccessScope().isPrivateScope() && (s.getSiloGroups() == null || s.getSiloGroups().isEmpty())) {
				list.add(s);
			}
		}
		return list;
	}

	/**
	 * Get all the silos that are public (excluding the examples) and that do not belong to a SiloGroup.
	 * @param type
	 * @return
	 */
	public List<Silo> getPublicSilos(GroupType type) {
		List<Silo> list = new ArrayList<>();
		List<Silo> tempList = getSilos(type);
		for (Silo s : tempList) {
			if ((s.getAccessScope().isPublicScope() || s.getAccessScope().isTeamScope()) && !s.isExample() && (s.getSiloGroups() == null || s.getSiloGroups().isEmpty())) {
				list.add(s);
			}
		}
		return list;
	}

	/**
	 * Get all the silos that are examples and that do not belong to a SiloGroup.
	 * @param type
	 * @return
	 */
	public List<Silo> getExampleSilos(GroupType type) {
		List<Silo> list = new ArrayList<>();
		List<Silo> tempList = getSilos(type);
		for (Silo s : tempList) {
			if (s.getAccessScope().isPublicScope() && s.isExample() && (s.getSiloGroups() == null || s.getSiloGroups().isEmpty())) {
				list.add(s);
			}
		}
		return list;
	}

	/**
	 * Returns a list of all silos (private, public, ...) that the user can read from.
	 *
	 * @return
	 */
	public List<Silo> getReadableSilos() {
		return streamableList(getSilos()).stream()
				.filter(silo -> SecurityService.getInstance().checkPersonalObject(Action.READ, silo))
				.collect(Collectors.toList());
	}

	/**
	 * Returns a list of all silos (private, public, ...) that the user can write to.
	 *
	 * @return
	 */
	public List<Silo> getWritableSilos() {
		return streamableList(getSilos()).stream()
				.filter(silo -> SecurityService.getInstance().checkPersonalObject(Action.UPDATE, silo))
				.collect(Collectors.toList());
	}

	public Silo createSilo(ProtocolClass pClass) {
		return createSilo(pClass, GroupType.WELL);
	}

	public Silo createSilo(ProtocolClass pClass, GroupType type) {
		// Create DB Object.
		Silo silo = new Silo();
		silo.setName("New silo");
		silo.setCreationDate(new Date());
		silo.setProtocolClass(pClass);
		silo.setOwner(SecurityService.getInstance().getCurrentUserName());
		silo.setType(type.getType());
		silo.setAccessScope(AccessScope.PRIVATE);

		return silo;
	}

	private Silo duplicateSilo(Silo silo) {
		// Create DB Object based on existing one.
		Silo siloCopy = new Silo();
		siloCopy.setName(silo.getName() + " Copy");
		siloCopy.setDescription(silo.getDescription());
		siloCopy.setCreationDate(new Date());
		siloCopy.setProtocolClass(silo.getProtocolClass());
		siloCopy.setOwner(SecurityService.getInstance().getCurrentUserName());
		siloCopy.setType(silo.getType());
		siloCopy.setAccessScope(silo.getAccessScope());
		siloCopy.setExample(silo.isExample());

		return siloCopy;
	}

	public void updateSilo(Silo silo) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, silo);
		save(silo);
	}

	public Silo duplicate(Silo silo) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.READ, silo);
		Silo siloCopy = duplicateSilo(silo);
		save(siloCopy);
		try {
			SiloDataService.getInstance().copyDataFile(silo, siloCopy);
		} catch (IOException e) {
			EclipseLog.error("Failed to update silo data file", e, Activator.getDefault());
		}
		return siloCopy;
	}

	public void deleteSilo(Silo silo) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.DELETE, silo);
		delete(silo);
		try {
			SiloDataService.getInstance().deleteDataFile(silo);
		} catch (IOException e) {
			EclipseLog.error("Failed to delete silo data file", e, Activator.getDefault());
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends PlatformObject> ISiloAccessor<T> getSiloAccessor(Silo silo) {
		if (!siloAccessors.containsKey(silo)) {
			if (silo.getType() == GroupType.WELL.getType()) {
				siloAccessors.put(silo, new WellSiloAccessor(silo));
			} else if (silo.getType() == GroupType.SUBWELL.getType()) {
				siloAccessors.put(silo, new SubWellSiloAccessor(silo));
			} else {
				throw new IllegalArgumentException("Unsupported silo type: " + silo.toString());
			}
		}
		return (ISiloAccessor<T>) siloAccessors.get(silo);
	}

	/*
	 * ***********************
	 * Public API - Silo Group
	 * ***********************
	 */

	public SiloGroup createSiloGroup(ProtocolClass pClass, GroupType type, AccessScope scope) {
		// Create DB Object.
		SiloGroup group = new SiloGroup();
		group.setName("New silo Group");
		group.setCreationDate(new Date());
		group.setProtocolClass(pClass);
		group.setOwner(SecurityService.getInstance().getCurrentUserName());
		group.setType(type.getType());
		group.setAccessScope(scope);
		return group;
	}

	public SiloGroup createSiloGroup(Silo silo) {
		return createSiloGroup(silo.getProtocolClass(), GroupType.getGroupType(silo.getType()), silo.getAccessScope());
	}

	public void updateSiloGroup(SiloGroup group) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, group);
		save(group);
	}

	public void deleteSiloGroup(SiloGroup group) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.DELETE, group);
		delete(group);
	}

	public List<SiloGroup> getSiloGroups() {
		return getList(SiloGroup.class);
	}

	public List<SiloGroup> getSiloGroups(GroupType type) {
		String query = "select sg from SiloGroup sg where sg.type = ?1";
		return getList(query, SiloGroup.class, type.getType());
	}

	public List<SiloGroup> getMySiloGroups(GroupType type) {
		String currentUser = SecurityService.getInstance().getCurrentUserName();
		String query = "select sg from SiloGroup sg where sg.owner = ?1 and sg.type = ?2";
		return getList(query, SiloGroup.class, currentUser, type.getType());
	}

	/**
	 * Get all the silogroups for the current user that are private.
	 * @param type
	 * @return
	 */
	public List<SiloGroup> getMyPrivateSiloGroups(GroupType type) {
		List<SiloGroup> list = new ArrayList<>();
		List<SiloGroup> tempList = getMySiloGroups(type);
		for (SiloGroup s : tempList) {
			if (s.getAccessScope().isPrivateScope()) {
				list.add(s);
			}
		}
		return list;
	}

	/**
	 * Get all the silogroups that are public (excluding the examples).
	 * @param type
	 * @return
	 */
	public List<SiloGroup> getPublicSiloGroups(GroupType type) {
		List<SiloGroup> list = new ArrayList<>();
		List<SiloGroup> tempList = getSiloGroups(type);
		for (SiloGroup s : tempList) {
			if ((s.getAccessScope().isPublicScope() || s.getAccessScope().isTeamScope()) && !s.isExample()) {
				list.add(s);
			}
		}
		return list;
	}

	/**
	 * Get all the silogroups that are examples.
	 * @param type
	 * @return
	 */
	public List<SiloGroup> getExampleSiloGroups(GroupType type) {
		List<SiloGroup> list = new ArrayList<>();
		List<SiloGroup> tempList = getSiloGroups(type);
		for (SiloGroup s : tempList) {
			if (s.getAccessScope().isPublicScope() && s.isExample()) {
				list.add(s);
			}
		}
		return list;
	}

	/*
	 * **************
	 * Event handling
	 * **************
	 */

	protected void fire(ModelEventType type, Object object, int status) {
		ModelEvent event = new ModelEvent(object, type, status);
		ModelEventService.getInstance().fireEvent(event);
	}

	@Override
	protected void afterSave(Object o) {
		if (o instanceof Silo) {
			// If the saved object is a Silo, make sure a data file is created if needed before triggering the event.
			try {
				SiloDataService.getInstance().createDataFile((Silo) o);
			} catch (IOException e) {
				EclipseLog.error("Failed to update silo data file", e, Activator.getDefault());
			}
		}
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