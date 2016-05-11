package eu.openanalytics.phaedra.model.protocol.util;

import eu.openanalytics.phaedra.base.db.IValueObject;

/**
 * Utility to generate a "qualified name" for any IValueObject.
 * A qualified name in this context means a String that represents the path of
 * the object in an IValueObject hierarchy.
 */
public class QualifiedNameGenerator {

	public static String getQualifiedName(IValueObject object) {
		String partName = "/" + object.toString();
		IValueObject parent = object.getParent();
		if (parent == null) return partName;
		else return getQualifiedName(parent) + partName;
	}
}
