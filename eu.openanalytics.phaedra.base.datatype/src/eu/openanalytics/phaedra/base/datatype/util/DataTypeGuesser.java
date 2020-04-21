package eu.openanalytics.phaedra.base.datatype.util;

import java.util.Date;

import eu.openanalytics.phaedra.base.datatype.description.BooleanValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.description.IntegerValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.RealValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.StringValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.TimestampDescription;


public class DataTypeGuesser {
	
	
	private final StringValueDescription stringValueDescription;
	private final BooleanValueDescription booleanValueDescription;
	private final IntegerValueDescription integerValueDescription;
	private final RealValueDescription realValueDescription;
	private final TimestampDescription timestampValueDescription;
	
	
	public DataTypeGuesser(final String name, final Class<?> entityType) {
		this.stringValueDescription = new StringValueDescription(name, entityType);
		this.booleanValueDescription = new BooleanValueDescription(name, entityType);
		this.integerValueDescription = new IntegerValueDescription(name, entityType);
		this.realValueDescription = new RealValueDescription(name, entityType);
		this.timestampValueDescription = new TimestampDescription(name, entityType);
	}
	
	public DataTypeGuesser() {
		this("", Object.class);
	}
	
	
	public DataDescription guessDataDescription(final Object value) {
		if (value instanceof Boolean) {
			return this.booleanValueDescription;
		}
		if (value instanceof Integer || value instanceof Long) {
			return this.integerValueDescription;
		}
		if (value instanceof Number) {
			return this.realValueDescription;
		}
		if (value instanceof Date) {
			return this.timestampValueDescription;
		}
		return this.stringValueDescription;
	}
	
}
