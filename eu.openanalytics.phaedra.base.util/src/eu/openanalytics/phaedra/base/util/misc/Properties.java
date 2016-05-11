package eu.openanalytics.phaedra.base.util.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenience class for a map of properties which can be serialized and/or cloned.
 * 
 * <br><b>IMPORTANT:</b> to allow binary serialization and deep cloning, all property values must implement Serializable!
 * <br><b>IMPORTANT:</b> to allow XML serialization, all property values must be JavaBean compliant!
 */
public class Properties implements Serializable {

	private static final long serialVersionUID = 1187542059221809437L;
	
	private Map<String, Object> mapProperty;

	public Properties() {
		this.mapProperty = new HashMap<>();
	}

	public void addProperty(String propertyName, Object object) {
		mapProperty.put(propertyName, object);
	}

	public void removeProperty(String propertyName) {
		mapProperty.remove(propertyName);
	}

	public Object getProperty(String propertyName) {
		return mapProperty.get(propertyName);
	}

	@SuppressWarnings("unchecked")
	public <T> T getProperty(String propertyName, T defaultValue) {
		Object o = mapProperty.get(propertyName);
		if (o != null) return (T) o;
		return defaultValue;
	}

	public <T> T getProperty(String propertyName, Class<T> clazz) {
		Object o = mapProperty.get(propertyName);
		if (o == null) return null;
		return SelectionUtils.getAsClass(o, clazz, true) ;
	}

	public Collection<String> keySet() {
		return mapProperty.keySet();
	}

	/*
	 * Below methods are for serialization purposes only. Do not use otherwise!
	 */

	public Map<String, Object> getMapProperty() {
		return mapProperty;
	}

	public void setMapProperty(Map<String, Object> mapProperty) {
		this.mapProperty = mapProperty;
	}

	public Properties deepClone() {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			new ObjectOutputStream(bos).writeObject(this);
			byte[] bytes = bos.toByteArray();
			Object clone = new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
			return (Properties) clone;
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to clone properties", e);
		}
	}
}
