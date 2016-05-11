package eu.openanalytics.phaedra.model.curve.util;

import java.util.Comparator;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class CurveComparators {

	public static Comparator<String> CENSOR_COMPARATOR = new Comparator<String>() {

		@Override
		public int compare(String o1, String o2) {
			if (o1 == null || o1.isEmpty()) return -1;
			if (o2 == null || o2.isEmpty()) return 1;

			if ("NaN".equals(o1)) return -1;
			if ("NaN".equals(o2)) return 1;

			char censor1 = '=';
			char censor2 = '=';
			String val1 = o1;
			String val2 = o2;
			if (o1.charAt(0) == '~' || o1.charAt(0) == '<' || o1.charAt(0) == '>') {
				censor1 = o1.charAt(0);
				val1 = o1.substring(1);
			}
			if (o2.charAt(0) == '~' || o2.charAt(0) == '<' || o2.charAt(0) == '>') {
				censor2 = o2.charAt(0);
				val2 = o2.substring(1);
			}

			boolean o1Numeric = NumberUtils.isDouble(val1);
			boolean o2Numeric = NumberUtils.isDouble(val2);

			int censorValue1 = 0;
			if (censor1 == '~') censorValue1 = 1;
			if (censor1 == '=') censorValue1 = 2;
			if (censor1 == '>') censorValue1 = 3;

			int censorValue2 = 0;
			if (censor2 == '~') censorValue2 = 1;
			if (censor2 == '=') censorValue2 = 2;
			if (censor2 == '>') censorValue2 = 3;

			if (o1Numeric && o2Numeric) {
				double d1 = Double.parseDouble(val1);
				double d2 = Double.parseDouble(val2);
				if (d1 < d2) return -1;
				if (d1 > d2) return 1;
				if (d1 == d2) {
					return censorValue1 - censorValue2;
				}
			}

			if (!o1Numeric) return -1;
			if (!o2Numeric) return 1;
			return o1.compareTo(o2);
		}

	};

	public static Comparator<String> NUMERIC_STRING_COMPARATOR = new Comparator<String>() {

		@Override
		public int compare(String o1, String o2) {
			boolean o1Numeric = NumberUtils.isDouble(o1);
			boolean o2Numeric = NumberUtils.isDouble(o2);
			if (o1Numeric && o2Numeric) {
				double d1 = Double.parseDouble(o1);
				double d2 = Double.parseDouble(o2);
				if (d1 == d2) return 0;
				if (d1 < d2) return -1;
				if (d1 > d2) return 1;
			}
			if (!o1Numeric) return -1;
			if (!o2Numeric) return 1;
			return o1.compareTo(o2);
		}

	};
}
