package eu.openanalytics.phaedra.base.db.jpa.converter;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

import eu.openanalytics.phaedra.base.db.JDBCUtils;

public class XMLConverter implements Converter {

	private static final long serialVersionUID = -3257158958775526736L;

	@Override
	public Object convertDataValueToObjectValue(Object dataValue, Session session) {
		return dataValue.toString();
	}

	@Override
	public Object convertObjectValueToDataValue(Object objectValue, Session session) {
		// Oracle can handle Strings by default.
		if (JDBCUtils.isOracle()) return objectValue;
		else return JDBCUtils.getXMLObjectParameter(objectValue.toString());
	}

	@Override
	public void initialize(DatabaseMapping mapping, Session session) {
		// Do nothing.
	}

	@Override
	public boolean isMutable() {
		return false;
	}

}
