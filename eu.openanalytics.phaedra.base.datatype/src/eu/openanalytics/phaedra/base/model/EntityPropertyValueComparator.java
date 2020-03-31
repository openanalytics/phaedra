package eu.openanalytics.phaedra.base.model;

import java.util.Comparator;


public class EntityPropertyValueComparator<T> implements Comparator<T> {
	
	
	private final EntityProperty<T> property;
	
	
	public EntityPropertyValueComparator(final EntityProperty<T> property) {
		this.property = property;
	}
	
	
	@Override
	@SuppressWarnings({ "rawtypes" })
	public int compare(final T o1, final T o2) {
		final Object v1 = this.property.getTypedValue(o1);
		final Object v2 = this.property.getTypedValue(o2);
		if (v1 == v2) {
			return 0;
		}
		if (v1 == null) {
			return 1;
		}
		if (v2 == null) {
			return -1;
		}
		return compareValues(v1, v2);
	}
	
	@SuppressWarnings("unchecked")
	protected int compareValues(final Object v1, final Object v2) {
		if (v1 instanceof Comparable && v2 instanceof Comparable) {
			return ((Comparable)v1).compareTo(v2);
		}
		else {
			return v1.toString().compareTo(v2.toString());
		}
	}
	
}
