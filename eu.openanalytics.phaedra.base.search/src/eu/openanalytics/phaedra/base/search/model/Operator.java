package eu.openanalytics.phaedra.base.search.model;

import static eu.openanalytics.phaedra.base.search.model.Operator.OperatorType.BOOLEAN;
import static eu.openanalytics.phaedra.base.search.model.Operator.OperatorType.NATURAL_NUMERIC;
import static eu.openanalytics.phaedra.base.search.model.Operator.OperatorType.REAL_NUMERIC;
import static eu.openanalytics.phaedra.base.search.model.Operator.OperatorType.STRING;
import static eu.openanalytics.phaedra.base.search.model.Operator.OperatorType.TEMPORAL;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import org.eclipse.core.runtime.PlatformObject;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;

import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public enum Operator {
	EQUALS("equal to", new OperatorType[] {NATURAL_NUMERIC, REAL_NUMERIC, TEMPORAL}),
	STRING_EQUALS("equal to", new OperatorType[] {STRING}),
	LIKE("like", new OperatorType[] {STRING}),
	STARTS_WITH("starting with", new OperatorType[] {STRING}),
	ENDS_WITH("ending with", new OperatorType[] {STRING}),
	LESS_THAN("less than", new OperatorType[] {NATURAL_NUMERIC, REAL_NUMERIC, TEMPORAL}),
	GREATER_THAN("greater than", new OperatorType[] {NATURAL_NUMERIC, REAL_NUMERIC, TEMPORAL}),
	EMPTY("empty", new OperatorType[] {STRING, NATURAL_NUMERIC, REAL_NUMERIC, TEMPORAL}),
	TRUE("true", new OperatorType[] {BOOLEAN}),
	BETWEEN("between", new OperatorType[] {NATURAL_NUMERIC, REAL_NUMERIC, TEMPORAL}),
	STRING_IN("in", new OperatorType[] {STRING}),
	IN("in", new OperatorType[] {NATURAL_NUMERIC, REAL_NUMERIC}),
	IN_LAST("in last", new OperatorType[] {TEMPORAL})
	;
	
	private String name;
	private OperatorType[] operatorTypes;
	
	public enum OperatorType {
		STRING,
		NATURAL_NUMERIC,
		REAL_NUMERIC,
		TEMPORAL,
		BOOLEAN
		;
		
		public static OperatorType getOperatorTypeFor(Field field) {
			if (ReflectionUtils.isCompatibleField(field, false, true, true, Arrays.asList(new Class<?>[] {Integer.class, Long.class}))) {
				return NATURAL_NUMERIC;
			}
			if (ReflectionUtils.isCompatibleField(field, false, true, true, Arrays.asList(new Class<?>[] {Float.class, Double.class}))) {
				return REAL_NUMERIC;
			}
			if (ReflectionUtils.isCompatibleField(field, false, true, true, Arrays.asList(new Class<?>[] {Boolean.class}))) {
				return BOOLEAN;
			}
			if (ReflectionUtils.isCompatibleField(field, false, true, true, Arrays.asList(new Class<?>[] {String.class}))) {
				return STRING;
			}
			if (ReflectionUtils.isCompatibleField(field, false, true, true, Arrays.asList(new Class<?>[] {Date.class}))) {
				return TEMPORAL;
			}
			return null;	 
		}
	}
	
	public enum DateOffsetType {
		DAYS("days"),
		WEEKS("weeks"),
		MONTHS("months"),
		YEARS("years");
		
		private String name;
		
		private DateOffsetType(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
	}
	
	private Operator(String name, OperatorType[] operatorTypes) {
		this.name = name;
		this.operatorTypes = operatorTypes;
	}
	
	public String getName() {
		return name;
	}

	public OperatorType[] getOperatorTypes() {
		return operatorTypes;
	}
	
	@SuppressWarnings("unchecked")
	public List<String> validate(Serializable value) {
		List<String> errorMessages = new ArrayList<>();
		switch(this) {
		case EQUALS:
		case LESS_THAN:
		case GREATER_THAN:
		case IN_LAST:
			if (value == null) {
				errorMessages.add("Value should not be empty for operator \"" + name + "\"");
			}
			break;
		case STRING_EQUALS:
		case LIKE:
		case STARTS_WITH:
		case ENDS_WITH:
			if (Strings.isNullOrEmpty((String) value)) {
				errorMessages.add("Value should not be empty for operator \"" + name + "\"");
			}
			break;
		case EMPTY:
		case TRUE:
			break;
		case BETWEEN:
			if (value == null || !(value instanceof Comparable[]) || ((Comparable[]) value).length != 2 || ((Comparable[]) value)[0] == null || ((Comparable[]) value)[1] == null) {
				errorMessages.add("2 values should be filled for operator \"" + name + "\"");
				break;
			}
			if (((Comparable[]) value)[0].compareTo(((Comparable[]) value)[1]) >= 0) {
				errorMessages.add("First value should be before second value for operator \"" + name + "\"");
			}
			break;
		case STRING_IN:
			if (value == null || !(value instanceof ArrayList<?>) || ((ArrayList<?>) value).isEmpty() || ((ArrayList<?>) value).size() == 1 && ((ArrayList<?>) value).get(0).equals("")) {
				errorMessages.add("At least 1 value should be filled for operator \"" + name + "\"");
			}
			break;
		case IN:
			if (value == null || !(value instanceof ArrayList<?>) || ((ArrayList<?>) value).isEmpty()) {
				errorMessages.add("At least 1 value should be filled for operator \"" + name + "\"");
			}
			break;
		default:
			throw new UnsupportedOperationException("No this operation defined for this operator:"  + this);
		}
		return errorMessages;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Predicate execute(CriteriaBuilder builder, From<? extends PlatformObject, ? extends PlatformObject> from, String field, boolean positive, boolean caseSensitive, Serializable value) {
		switch(this) {
		case EQUALS:
			return positive 
					? builder.equal(from.get(field), value)
					: builder.notEqual(from.get(field), value);
		case STRING_EQUALS:
			return positive 
					? caseSensitive
							? builder.equal(from.<String>get(field), value)
							: builder.equal(builder.lower(from.<String>get(field)), ((String) value).toLowerCase())
					: caseSensitive
							? builder.notEqual(from.get(field), value)
							: builder.notEqual(builder.lower(from.<String>get(field)), ((String) value).toLowerCase());
		case LIKE:
			return positive
					? caseSensitive
							? builder.like(from.<String>get(field), "%" + value + "%")
							: builder.like(builder.lower(from.<String>get(field)), "%" + ((String) value).toLowerCase() + "%")
					: caseSensitive
							? builder.notLike(from.<String>get(field), "%" + value + "%")
							: builder.notLike(builder.lower(from.<String>get(field)), "%" + ((String) value).toLowerCase() + "%");
		case STARTS_WITH:
			return positive
					? caseSensitive
							? builder.like(from.<String>get(field), value + "%")
							: builder.like(builder.lower(from.<String>get(field)), ((String) value).toLowerCase() + "%")
					: caseSensitive
							? builder.notLike(from.<String>get(field), value + "%")
							: builder.notLike(builder.lower(from.<String>get(field)), ((String) value).toLowerCase() + "%");
		case ENDS_WITH:
			return positive
					? caseSensitive
							? builder.like(from.<String>get(field), "%" + value)
							: builder.like(builder.lower(from.<String>get(field)), "%" + ((String) value).toLowerCase())
					: caseSensitive
							? builder.notLike(from.<String>get(field), "%" + value)
							: builder.notLike(builder.lower(from.<String>get(field)), "%" + ((String) value).toLowerCase());
		case LESS_THAN:
			return positive 
					? builder.lessThan(from.<Comparable>get(field), (Comparable) value)
					: builder.greaterThanOrEqualTo(from.<Comparable>get(field), (Comparable) value);
		case GREATER_THAN:
			return positive
					? builder.greaterThan(from.<Comparable>get(field), (Comparable) value)
					: builder.lessThanOrEqualTo(from.<Comparable>get(field), (Comparable) value);
		case EMPTY:
			return positive 
					? builder.isNull(from.get(field))
					: builder.isNotNull(from.get(field));
		case TRUE:
			return positive 
					? builder.isTrue(from.<Boolean>get(field))
					: builder.isFalse(from.<Boolean>get(field));
		case BETWEEN:
			return positive
					? builder.between(from.<Comparable>get(field), ((Comparable[]) value)[0], ((Comparable[]) value)[1])
					: builder.not(builder.between(from.<Comparable>get(field), ((Comparable[]) value)[0], ((Comparable[]) value)[1]));
		case STRING_IN:
			return positive
					? caseSensitive
							? from.<String>get(field).in((Collection<String>) value)
							: builder.lower(from.<String>get(field)).in(toLower((Collection<String>) value))
					: caseSensitive
							? builder.not(from.<String>get(field).in((Collection<String>) value))
							: builder.not(builder.lower(from.<String>get(field)).in(toLower((Collection<String>) value)));
		case IN:
			return positive
					? from.get(field).in((Collection<String>) value)
					: builder.not(from.get(field).in((Collection<String>) value));
		case IN_LAST:
			return positive 
					? builder.greaterThanOrEqualTo(from.<Date>get(field), calculateOffsetDate(new Date(), (DateOffsetType) ((Serializable[]) value)[0], (Integer) ((Serializable[]) value)[1]))
					: builder.lessThan(from.<Date>get(field), (Date) calculateOffsetDate(new Date(), (DateOffsetType) ((Serializable[]) value)[0], (Integer) ((Serializable[]) value)[1]));							
		default:
			throw new UnsupportedOperationException("No this operation defined for this operator:"  + this); 	
		}
	}
	
	private static Collection<String> toLower(Collection<String> strings) {
		return Collections2.transform(strings, new Function<String, String>() {
			@Override
			public String apply(String string) {
				return string.toLowerCase();
			}			
		});
	}

	public static Set<Operator> getOperators(final OperatorType operatorType) {
		return new HashSet<>(Collections2.filter(Arrays.asList(Operator.values()), new com.google.common.base.Predicate<Operator>() {
			@Override
			public boolean apply(Operator operator) {
				return Arrays.asList(operator.getOperatorTypes()).contains(operatorType);
			}
		}));
	}
	
	public static Date calculateOffsetDate(Date fromDate, DateOffsetType dateOffsetType, int amount) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(fromDate);

		switch (dateOffsetType) {
		case DAYS:			
			calendar.add(Calendar.DAY_OF_YEAR, -amount);
			break;
		case WEEKS:			
			calendar.add(Calendar.WEEK_OF_YEAR, -amount);
			break;
		case MONTHS:			
			calendar.add(Calendar.MONTH, -amount);
			break;
		case YEARS:			
			calendar.add(Calendar.YEAR, -amount);
			break;
		default:
			throw new UnsupportedOperationException("No this operation defined for this dateOffsetType:"  + dateOffsetType);
		}
		return calendar.getTime();
	}
}
