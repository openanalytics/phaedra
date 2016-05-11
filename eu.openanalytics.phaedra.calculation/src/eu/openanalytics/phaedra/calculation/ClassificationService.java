package eu.openanalytics.phaedra.calculation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

/**
 * This service provides the following classification functions
 * on the well and sub-well level:
 *
 * <ul>
 * <li>Obtain well or sub-well features that have classification.</li>
 * <li>Obtain the accept/reject classification feature for sub-well data.</li>
 * <li>Get wells or sub-well items that match a feature class.</li>
 * <li>Get the highest feature class that a well or sub-well item matches.</li>
 * </ul>
 *
 */
public class ClassificationService {

	private static ClassificationService instance = new ClassificationService();

	private final static String REJECTION_FEATURE_NAME = "Rejected";
	private final static String REJECTION_CLASS_REJECTED = "Rejected";
	private final static String REJECTION_CLASS_ACCEPTED = "Accepted";

	private ClassificationService() {
		// Hidden constructor.
	}

	public static ClassificationService getInstance() {
		return instance;
	}

	/*
	 * **********
	 * Public API
	 * **********
	 */

	public List<Feature> findWellClassificationFeatures(ProtocolClass pClass) {
		return ProtocolService.streamableList(pClass.getFeatures()).stream()
				.filter(f -> !f.getFeatureClasses().isEmpty())
				.collect(Collectors.toList());
	}

	public List<SubWellFeature> findSubWellClassificationFeatures(ProtocolClass pClass) {
		return ProtocolService.streamableList(pClass.getSubWellFeatures()).stream()
				.filter(f -> !f.getFeatureClasses().isEmpty())
				.collect(Collectors.toList());
	}

	public SubWellFeature findRejectionFeature(ProtocolClass pClass) {
		return ProtocolService.streamableList(pClass.getSubWellFeatures()).stream()
				.filter(f -> f.getName().equals(REJECTION_FEATURE_NAME))
				.findAny().orElse(null);
	}

	public FeatureClass findRejectionClass(SubWellFeature rejectionFeature) {
		if (rejectionFeature == null) return null;
		return ProtocolService.streamableList(rejectionFeature.getFeatureClasses()).stream()
				.filter(fc -> fc.getLabel().equals(REJECTION_CLASS_REJECTED))
				.findAny().orElse(null);
	}

	public FeatureClass findAcceptedClass(SubWellFeature rejectionFeature) {
		if (rejectionFeature == null) return null;
		return ProtocolService.streamableList(rejectionFeature.getFeatureClasses()).stream()
				.filter(fc -> fc.getLabel().equals(REJECTION_CLASS_ACCEPTED))
				.findAny().orElse(null);
	}

	/**
	 * Find the feature classes a well belongs to for a given feature.
	 *
	 * @param well The well to test.
	 * @param feature The feature containing classification.
	 * @return The matching feature classes.
	 */
	public List<FeatureClass> getFeatureClasses(Well well, Feature feature) {
		PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
		if (feature.isNumeric()) {
			double value = dataAccessor.getNumericValue(well, feature, null);
			return getFeatureClasses(value, feature);
		} else {
			String value = dataAccessor.getStringValue(well, feature);
			return getFeatureClasses(value, feature);
		}
	}

	/**
	 * Find the feature classes a sub-well entity belongs to for a given sub-well feature.
	 *
	 * @param well The parent well of the sub-well data item.
	 * @param entityIndex The index of the sub-well data item.
	 * @param feature The sub-well feature containing classification.
	 * @return The matching feature classes.
	 */
	public List<FeatureClass> getFeatureClasses(Well well, int entityIndex, SubWellFeature feature) {
		if (feature.isNumeric()) {
			float[] data = SubWellService.getInstance().getNumericData(well, feature);
			if (data == null || entityIndex >= data.length) return new ArrayList<FeatureClass>();
			float value = data[entityIndex];
			return getFeatureClasses(value, feature);
		} else {
			String[] data = SubWellService.getInstance().getStringData(well, feature);
			if (data == null || entityIndex >= data.length) return new ArrayList<FeatureClass>();
			String value = data[entityIndex];
			return getFeatureClasses(value, feature);	
		}
	}

	/**
	 * Find the highest class a well matches.
	 * The meaning of "highest" depends on the classification type, for bit patterns it is the highest numeric value.
	 *
	 * @param well The well to test.
	 * @param feature The feature containing classification.
	 * @return The highest matching class, or null if no class matches.
	 */
	public FeatureClass getHighestClass(Well well, Feature feature) {
		List<FeatureClass> matchingClasses = getFeatureClasses(well, feature);
		return getHighest(matchingClasses);
	}

	/**
	 * Find the highest feature class an entity matches.
	 * The meaning of "highest" depends on the classification type, for bit patterns it is the highest numeric value.
	 *
	 * @param well The parent well of the sub-well data item.
	 * @param entityIndex The index of the sub-well data item.
	 * @param feature The sub-well feature containing classification.
	 * @return The highest matching class, or null if no class matches.
	 */
	public FeatureClass getHighestClass(Well well, int entityIndex, SubWellFeature feature) {
		List<FeatureClass> matchingClasses = getFeatureClasses(well, entityIndex, feature);
		return getHighest(matchingClasses);
	}

	public FeatureClass getHighestClass(double value, IFeature feature) {
		List<FeatureClass> matchingClasses = getFeatureClasses(value, feature);
		return getHighest(matchingClasses);
	}
	
	public FeatureClass getHighestClass(String value, IFeature feature) {
		List<FeatureClass> matchingClasses = getFeatureClasses(value, feature);
		return getHighest(matchingClasses);
	}
	
	/**
	 * Test whether a well matches a feature class or not.
	 *
	 * @param well The well to test.
	 * @param fClass The feature class to test against.
	 * @return True if the well matches.
	 */
	public boolean matchesClass(Well well, FeatureClass fClass) {
		Feature f = fClass.getWellFeature();
		PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
		if (f.isNumeric()) {
			double value = dataAccessor.getNumericValue(well, f, null);
			return PatternType.getType(fClass).matches(fClass.getPattern(), value);
		} else {
			String value = dataAccessor.getStringValue(well, f);
			return PatternType.getType(fClass).matches(fClass.getPattern(), value);
		}
	}

	/**
	 * Test whether a sub-well item matches a feature class or not.
	 *
	 * @param well The parent well of the sub-well item.
	 * @param item The index of the item to test.
	 * @param fClass The feature class to test against.
	 * @return True if the well matches.
	 */
	public boolean matchesClass(Well well, int item, FeatureClass fClass) {
		if (fClass.getSubWellFeature().isNumeric()) {
			float[] data = SubWellService.getInstance().getNumericData(well, fClass.getSubWellFeature());
			if (data == null) return false;
			return PatternType.getType(fClass).matches(fClass.getPattern(), data[item]);
		} else {
			String[] data = SubWellService.getInstance().getStringData(well, fClass.getSubWellFeature());
			if (data == null) return false;
			return PatternType.getType(fClass).matches(fClass.getPattern(), data[item]);
		}
	}

	/**
	 * Test whether a well matches at least one of the given feature classes.
	 *
	 * @param well The well to test.
	 * @param feature The classification feature containing the classes.
	 * @param fClasses The feature classes to test against.
	 * @param unclassifiedMatches If true, an unclassified well (i.e. matches no class at all) will return true.
	 * @return True if the well matches one or more of the given feature classes.
	 */
	public boolean matchesClasses(Well well, Feature feature, List<FeatureClass> fClasses, boolean unclassifiedMatches) {
		PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
		if (feature.isNumeric()) {
			double value = dataAccessor.getNumericValue(well, feature, null);
			if (Double.isNaN(value)) return unclassifiedMatches;
			for (FeatureClass fc : fClasses) {
				if (PatternType.getType(fc).matches(fc.getPattern(), value)) return true;
			}
			if (unclassifiedMatches) return getFeatureClasses(value, feature).isEmpty();
			return false;
		} else {
			String value = dataAccessor.getStringValue(well, feature);
			for (FeatureClass fc : fClasses) {
				if (PatternType.getType(fc).matches(fc.getPattern(), value)) return true;
			}
			if (unclassifiedMatches) return getFeatureClasses(value, feature).isEmpty();
			return false;
		}
	}

	/**
	 * Test whether a sub-well item matches at least one of the given feature classes.
	 *
	 * @param well The parent well of the sub-well item.
	 * @param item the index of the sub-well item to test.
	 * @param feature The classification feature containing the classes.
	 * @param fClasses The feature classes to test against.
	 * @param unclassifiedMatches If true, an unclassified sub-well item (i.e. matches no class at all) will return true.
	 * @return True if the sub-well item matches one or more of the given feature classes.
	 */
	public boolean matchesClasses(Well well, int item, SubWellFeature feature, List<FeatureClass> fClasses, boolean unclassifiedMatches) {
		if (feature.isNumeric()) {
			float[] data = SubWellService.getInstance().getNumericData(well, feature);
			if (data == null || item >= data.length) return unclassifiedMatches;
			float value = data[item];
			for (FeatureClass fc : fClasses) {
				if (PatternType.getType(fc).matches(fc.getPattern(), value)) return true;
			}
			if (unclassifiedMatches) return getFeatureClasses(value, feature).isEmpty();
			return false;
		} else {
			String[] data = SubWellService.getInstance().getStringData(well, feature);
			if (data == null || item >= data.length) return unclassifiedMatches;
			String value = data[item];
			for (FeatureClass fc : fClasses) {
				if (PatternType.getType(fc).matches(fc.getPattern(), value)) return true;
			}
			if (unclassifiedMatches) return getFeatureClasses(value, feature).isEmpty();
			return false;
		}
	}
	
	/**
	 * Get a numeric representation of the given FeatureClass.
	 * This is mainly used for applying feature classes to objects, and for sorting feature classes.
	 */
	public int getNumericRepresentation(FeatureClass fClass) {
		return PatternType.getType(fClass).getNumericRepresentation(fClass);
	}
	
	/**
	 * Get a String representation of the given FeatureClass.
	 * This is mainly used for applying feature classes to objects, and for sorting feature classes.
	 */
	public String getStringRepresentation(FeatureClass fClass) {
		return PatternType.getType(fClass).getStringRepresentation(fClass);
	}

	public static enum PatternType {
		
		BitMask("bit", "The pattern describes a bit mask. Wildcards are allowed in the form of dots."),
		Literal("literal", "The pattern describes a literal value, either string or numeric.");
		
		private String name;
		private String description;
		
		private static final PatternType DEFAULT_TYPE = PatternType.Literal;
		
		PatternType(String name, String description) {
			this.name = name;
			this.description = description;
		}
		
		public String getName() {
			return name;
		}
		
		public String getDescription() {
			return description;
		}
		
		public int getNumericRepresentation(FeatureClass fClass) {
			String pattern = fClass.getPattern();
			if (this == BitMask) {
				return Integer.parseInt(pattern.replace('.', '0'), 2);
			} else {
				if (NumberUtils.isDigit(pattern)) return Integer.parseInt(pattern);
				else return 0;
			}
		}
		
		public String getStringRepresentation(FeatureClass fClass) {
			return fClass.getPattern();
		}
		
		public boolean matches(String pattern, double value) {
			if (this == BitMask) {
				if (Double.isNaN(value)) return false;
				int entityValue = (int)value;
				int len = pattern.length();
				for (int i=0; i<len; i++) {
					char patternChar = pattern.charAt(len-i-1);
					if (patternChar == '.') continue;
					char c = (char)(48 + ((entityValue >> i) & 0x1));
					if (patternChar != c) return false;
				}
				return true;
			} else {
				if (NumberUtils.isDouble(pattern)) {
					Double patternValue = Double.parseDouble(pattern);
					patternValue.equals(value);
				}
				return false;
			}
		}
		
		public boolean matches(String pattern, String value) {
			if (this == BitMask) {
				if (NumberUtils.isDouble(value)) return matches(pattern, Double.parseDouble(value));
				return false;
			} else {
				return pattern.equals(value);
			}
		}
		
		public static PatternType getType(FeatureClass clazz) {
			for (PatternType type: values()) {
				if (type.getName().equals(clazz.getPatternType())) return type;
			}
			return DEFAULT_TYPE;
		}
	}
	
	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private List<FeatureClass> getFeatureClasses(double value, IFeature feature) {
		return ProtocolService.streamableList(feature.getFeatureClasses()).stream()
				.filter(fc -> PatternType.getType(fc).matches(fc.getPattern(), value))
				.collect(Collectors.toList());
	}
	
	private List<FeatureClass> getFeatureClasses(String value, IFeature feature) {
		return ProtocolService.streamableList(feature.getFeatureClasses()).stream()
				.filter(fc -> PatternType.getType(fc).matches(fc.getPattern(), value))
				.collect(Collectors.toList());
	}
	
	private FeatureClass getHighest(List<FeatureClass> classes) {
		int highest = 0;
		FeatureClass highestClass = null;
		for (FeatureClass c: classes) {
			int v = getNumericRepresentation(c);
			if (v >= highest) {
				highest = v;
				highestClass = c;
			}
		}
		return highestClass;
	}


}