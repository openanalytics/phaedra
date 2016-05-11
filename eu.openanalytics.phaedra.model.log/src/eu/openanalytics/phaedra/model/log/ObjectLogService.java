package eu.openanalytics.phaedra.model.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;
import eu.openanalytics.phaedra.model.log.vo.ObjectLog;
import eu.openanalytics.phaedra.model.log.vo.ObjectLogEventType;

/**
 * This service enables the logging of changes to objects, including:
 * <ul>
 * <li>Plate validation</li>
 * <li>Well rejection</li>
 * <li>Changes to feature values (e.g. classification)</li>
 * </ul>
 * In addition, the recorded history of any object can be retrieved.
 */
public class ObjectLogService extends BaseJPAService {

	private static ObjectLogService instance = new ObjectLogService();
	
	private ObjectLogService() {
		// Hidden constructor.
	}
	
	public static ObjectLogService getInstance() {
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
	
	public List<ObjectLog> getHistory(Object object) {
		String clazz = object.getClass().getName();
		Object idVal = ReflectionUtils.invoke("getId", object);
		if (idVal == null) return new ArrayList<>();
		long id = (Long)idVal;

		List<ObjectLog> results = getList("select l from ObjectLog l where l.objectClass = ?1 and l.objectId = ?2", ObjectLog.class, clazz, id);
		Collections.sort(results, new Comparator<ObjectLog>() {
			@Override
			public int compare(ObjectLog o1, ObjectLog o2) {
				return o1.getTimestamp().compareTo(o2.getTimestamp());
			}
		});
		return results;
	}

	public void logChange(Object obj, String prop, String oldVal, String newVal, String remark) {
		ObjectLogEventType eventType = getEventType("CHANGED");
		log(obj, eventType, prop, null, oldVal, newVal, remark);
	}
	
	public void logFeatureChange(Object obj, String fName, String oldVal, String newVal, String remark) {
		ObjectLogEventType eventType = getEventType("CHANGED");
		log(obj, eventType, "Feature", fName, oldVal, newVal, remark);
	}
	
	public ObjectLog log(Object object, ObjectLogEventType eventType, String prop1, String prop2, String oldVal, String newVal, String remark) {
		if (object == null || eventType == null) return null;
		
		ObjectLog log = new ObjectLog();
		log.setEventType(eventType);
		log.setTimestamp(new Date());
		log.setUserCode(SecurityService.getInstance().getCurrentUserName());
		log.setObjectClass(object.getClass().getName());
		log.setObjectId((Long)ReflectionUtils.invoke("getId", object));
		log.setObjectProperty1(prop1);
		log.setObjectProperty2(prop2);
		log.setOldValue(oldVal);
		log.setNewValue(newVal);
		log.setRemark(remark);
		
		save(log);
		return log;
	}
	
	public List<ObjectLog> batchLogChange(List<?> objects, String prop, List<String> oldVals, String newVal, String remark) {
		ObjectLogEventType eventType = getEventType("CHANGED");
		
		List<ObjectLog> logs = new ArrayList<>();
		if (objects == null || objects.isEmpty()) return logs;
		
		for (int i = 0; i < objects.size(); i++) {
			ObjectLog log = new ObjectLog();
			log.setEventType(eventType);
			log.setTimestamp(new Date());
			log.setUserCode(SecurityService.getInstance().getCurrentUserName());
			log.setObjectClass(objects.get(i).getClass().getName());
			log.setObjectId((Long)ReflectionUtils.invoke("getId", objects.get(i)));
			log.setObjectProperty1(prop);
			log.setObjectProperty2(null);
			log.setOldValue(oldVals.get(i));
			log.setNewValue(newVal);
			log.setRemark(remark);
			logs.add(log);
		}

		saveCollection(logs);
		return logs;
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private ObjectLogEventType getEventType(String code) {
		return getEntity("select t from ObjectLogEventType t where t.code = ?1", ObjectLogEventType.class, code);
	}
	

}
