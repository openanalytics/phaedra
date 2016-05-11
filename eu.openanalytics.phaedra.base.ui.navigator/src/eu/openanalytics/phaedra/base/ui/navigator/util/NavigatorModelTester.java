package eu.openanalytics.phaedra.base.ui.navigator.util;

import org.eclipse.core.expressions.PropertyTester;

import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;

public class NavigatorModelTester extends PropertyTester {

	public final static String PROP_ELEMENT_DATA_CLASS = "elementDataClass";
	
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		
		IElement el = (IElement)receiver;
		
		if (property.equals(PROP_ELEMENT_DATA_CLASS)) {
			Object data = el.getData();
			if (data != null) {
				Class<?> clazz = data.getClass();
				String expectedClass = (String)expectedValue;
				return clazz.getName().equals(expectedClass);
			}
		}
		
		return false;
	}

}
