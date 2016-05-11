package eu.openanalytics.phaedra.base.ui.nattable.misc;

import java.util.Comparator;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;

public class DefaultAdvancedComparator implements Comparator<Object> {

	private static DefaultAdvancedComparator singleton;

	public static final DefaultAdvancedComparator getInstance() {
		if (singleton == null) {
			singleton = new DefaultAdvancedComparator();
		}
		return singleton;
	}

	@Override
	public int compare(final Object o1, final Object o2) {
		if (o1 == null) {
			if (o2 == null) {
				return 0;
			} else {
				return -1;
			}
		} else if (o2 == null) {
			return 1;
		}

		//if (o1 instanceof Comparable && o1.getClass() == o2.getClass()) {
		//	return ((Comparable) o1).compareTo(o2);
		//}

		String o2String = o2.toString();
		if (o1 instanceof Number) {
			if (NumberUtils.isDouble(o2String)) {
				if (o1 instanceof Integer && NumberUtils.isNumeric(o2String)) {
					Integer i1 = (Integer) o1;
					Integer i2 = Integer.valueOf(o2String);
					return i1.compareTo(i2);
				}
				if (o1 instanceof Long && NumberUtils.isNumeric(o2String)) {
					Long l1 = (Long) o1;
					Long l2 = Long.valueOf(o2String);
					return l1.compareTo(l2);
				}
				if (o1 instanceof Float) {
					Float f1 = (Float) o1;
					Float f2 = Float.valueOf(o2String);
					return f1.compareTo(f2);
				}
				if (o1 instanceof Double) {
					Double d1 = (Double) o1;
					Double d2 = Double.valueOf(o2String);
					return d1.compareTo(d2);
				}
			}
		}

		String o1String = o1.toString();
		if (NumberUtils.isNumeric(o1String) && NumberUtils.isNumeric(o2String)) {
			return StringUtils.compareToNumericStrings(o1String, o2String);
		}

		return o1String.compareTo(o2String);
	}

}
