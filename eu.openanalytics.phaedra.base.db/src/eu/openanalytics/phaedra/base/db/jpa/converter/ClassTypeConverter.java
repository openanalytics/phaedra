package eu.openanalytics.phaedra.base.db.jpa.converter;

import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.mappings.foundation.AbstractDirectMapping;
import org.eclipse.persistence.sessions.Session;

@SuppressWarnings("serial")
public class ClassTypeConverter implements Converter {
	protected DatabaseMapping mapping;
	
	@Override
	public Object convertDataValueToObjectValue(Object fieldValue, Session session) {
        if (fieldValue != null) {
            return (Class<?>)((AbstractSession)session).getDatasourcePlatform().convertObject(fieldValue, ClassConstants.CLASS);
         }

        return null;
	}

	@Override
	public Object convertObjectValueToDataValue(Object attributeValue, Session session) {
		if (attributeValue == null || !(attributeValue instanceof Class)) {
			return null;
		}
		return ((Class<?>)attributeValue).getName();
	}

	@Override
	public void initialize(DatabaseMapping mapping, Session session) {
		this.mapping = mapping;
        // CR#... Mapping must also have the field classification.
        if (getMapping().isDirectToFieldMapping()) {
            AbstractDirectMapping directMapping = (AbstractDirectMapping) getMapping();

            // Allow user to specify field type to override computed value. (i.e. blob, nchar)
            if (directMapping.getFieldClassification() == null) {
                directMapping.setFieldClassification(ClassConstants.STRING);
            }
        }
	}

	protected DatabaseMapping getMapping() {
        return mapping;
    }
	
	@Override
	public boolean isMutable() {		
		return false;
	}

}
