package eu.openanalytics.phaedra.base.db.jpa.converter;

import java.beans.DefaultPersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

import eu.openanalytics.phaedra.base.util.Activator;
import eu.openanalytics.phaedra.base.util.misc.Properties;

public class PropertiesConverter implements Converter {

	private static final long serialVersionUID = 3487321088374767288L;

	@Override
	public Object convertDataValueToObjectValue(Object dataValue, Session session) {
		if (dataValue instanceof String) {
			String propertiesAsString = (String) dataValue;
			return getStringAsProperties(propertiesAsString);
		}
		return new Properties();
	}

	@Override
	public Object convertObjectValueToDataValue(Object objectValue, Session session) {
		if (objectValue instanceof Properties) {
			Properties properties = (Properties) objectValue;
			return getPropertiesAsString(properties);
		}
		return objectValue;
	}

	@Override
	public void initialize(DatabaseMapping mapping, Session session) {
		// Do nothing.
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	private String getPropertiesAsString(Properties properties) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XMLEncoder xmlEncoder = new XMLEncoder(baos);
		// Fix for performance issue with xmlEncoder.getPersistenceDelegate(Properties.class);
		xmlEncoder.setPersistenceDelegate(Properties.class, new DefaultPersistenceDelegate());
		xmlEncoder.writeObject(properties);
		xmlEncoder.close();
		return baos.toString();
	}

	private Properties getStringAsProperties(String propertiesAsString) {
		ByteArrayInputStream baos = new ByteArrayInputStream(propertiesAsString.getBytes());
		XMLDecoder decoder = new XMLDecoder(baos, null, null, Activator.class.getClassLoader());
		Object o = decoder.readObject();
		decoder.close();
		return (Properties) o;
	}

}
