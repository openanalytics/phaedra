package eu.openanalytics.phaedra.base.datatype.description;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;


public class DataDescriptionUtils {
	
	
	private static final Collator NAME_COLLATOR = Collator.getInstance(Locale.ENGLISH);
	private static final Comparator<DataDescription> NAME_COMPARATOR = new Comparator<DataDescription>() {
		@Override
		public int compare(final DataDescription descr1, final DataDescription descr2) {
			return NAME_COLLATOR.compare(descr1.getName(), descr2.getName());
		}
	};
	
	public static Comparator<DataDescription> getNameComparator() {
		return NAME_COMPARATOR;
	}
	
	public static Comparator<DataDescription> getNameComparator(final DataUnitConfig dataUnitConfig) {
		return new Comparator<DataDescription>() {
			@Override
			public int compare(final DataDescription descr1, final DataDescription descr2) {
				return NAME_COLLATOR.compare(
						descr1.convertNameTo(descr1.getName(), dataUnitConfig), 
						descr2.convertNameTo(descr2.getName(), dataUnitConfig) );
			}
		}; 
	}
	
}
