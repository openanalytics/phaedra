package eu.openanalytics.phaedra.silo;

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
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.accessor.SubWellSiloAccessor;
import eu.openanalytics.phaedra.silo.accessor.WellSiloAccessor;
import eu.openanalytics.phaedra.silo.util.ObjectCopyFactory;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SiloService extends BaseJPAService {

	private static SiloService instance = new SiloService();

	private Map<Silo, ISiloAccessor<? extends PlatformObject>> siloAccessors;

	private SiloService() {
		// Hidden constructor
		this.siloAccessors = new ConcurrentHashMap<>();
	}

	public static SiloService getInstance() {
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

	public List<Silo> getPrivateSilos(GroupType type) {
		String currentUser = SecurityService.getInstance().getCurrentUserName();
		return getSilos(type).stream()
				.filter(s -> s.getAccessScope().isPrivateScope())
				.filter(s -> s.getOwner().equalsIgnoreCase(currentUser))
				.collect(Collectors.toList());
	}

	public List<Silo> getTeamSilos(GroupType type) {
		return getSilos(type).stream()
				.filter(s -> s.getAccessScope().isTeamScope())
				.filter(s -> SecurityService.getInstance().checkPersonalObject(Action.READ, s))
				.collect(Collectors.toList());
	}
	
	public List<Silo> getPublicSilos(GroupType type) {
		return getSilos(type).stream().filter(s -> s.getAccessScope().isPublicScope()).collect(Collectors.toList());
	}

	public List<Silo> getReadableSilos() {
		return getSilos().stream()
				.filter(silo -> SecurityService.getInstance().checkPersonalObject(Action.READ, silo))
				.collect(Collectors.toList());
	}

	public List<Silo> getWritableSilos() {
		return getSilos().stream()
				.filter(silo -> SecurityService.getInstance().checkPersonalObject(Action.UPDATE, silo))
				.collect(Collectors.toList());
	}

	private List<Silo> getSilos() {
		return streamableList(getList(Silo.class));
	}
	
	private List<Silo> getSilos(GroupType type) {
		String query = "select s from Silo s where s.type = ?1";
		return streamableList(getList(query, Silo.class, type.getType()));
	}
	
	public Silo createSilo(ProtocolClass pClass, GroupType type) {
		Silo silo = new Silo();
		silo.setName("New Silo");
		silo.setCreationDate(new Date());
		silo.setProtocolClass(pClass);
		silo.setOwner(SecurityService.getInstance().getCurrentUserName());
		silo.setType(type.getType());
		silo.setAccessScope(AccessScope.PRIVATE);
		return silo;
	}

	public void updateSilo(Silo silo) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, silo);
		save(silo);
	}

	public Silo cloneSilo(Silo silo) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.READ, silo);
		Silo copy = new Silo();
		ObjectCopyFactory.copy(silo, copy, false);
		copy.setName(silo.getName() + " Copy");
		copy.setCreationDate(new Date());
		copy.setOwner(SecurityService.getInstance().getCurrentUserName());
		save(copy);
		SiloDataService.getInstance().copyData(silo, copy);
		return copy;
	}

	public void deleteSilo(Silo silo) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.DELETE, silo);
		delete(silo);
		SiloDataService.getInstance().deleteData(silo);
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