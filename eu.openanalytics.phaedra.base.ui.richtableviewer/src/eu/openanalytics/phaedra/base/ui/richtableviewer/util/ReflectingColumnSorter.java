package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import java.util.Comparator;

import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class ReflectingColumnSorter implements Comparator<Object> {

	private String methodName;
	
	public ReflectingColumnSorter(String methodName) {
		this.methodName = methodName;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int compare(Object o1, Object o2) {
		Object field1 = ReflectionUtils.invoke(methodName, o1);
		Object field2 = ReflectionUtils.invoke(methodName, o2);
		if (field1 instanceof Comparable && field2 instanceof Comparable) {
			Comparable resolved1 = (Comparable)field1;
			Comparable resolved2 = (Comparable)field2;
			return resolved1.compareTo(resolved2);
		} else {
			if (field1 == null && field2 == null) return 0;
			if (field1 == null) return -1;
			if (field2 == null) return 1;
			return field1.toString().compareTo(field2.toString());
		}
	};
}
