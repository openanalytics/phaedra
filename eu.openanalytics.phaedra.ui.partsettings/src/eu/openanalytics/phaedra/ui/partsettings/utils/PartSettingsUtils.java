package eu.openanalytics.phaedra.ui.partsettings.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IInputValidator;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.partsettings.vo.PartSettings;

public class PartSettingsUtils {

	private static final String DELIMITER = ",";
	private static final String FEATURE_IDS = "FEATURE_IDS";
	private static final String NORMALIZATIONS = "NORMALIZATIONS";

	public final static Comparator<PartSettings> NAME_SORTER = (s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName());

	public final static IInputValidator NAME_INPUT_VALIDATOR = input -> {
		if (input.length() > 100) return "Please specify a name of 100 characters or less.";
		if (input.length() < 3) return "Please specify a name of at least 3 characters.";
		return null;
	};

	public static void setFeature(Properties properties, Feature feature) {
		setFeatures(properties, Arrays.asList(feature));
	}

	public static void setFeatures(Properties properties, List<Feature> features) {
		String featureIds = features.stream()
				.filter(Objects::nonNull)
				.map(f -> String.valueOf(f.getId()))
				.collect(Collectors.joining(DELIMITER));
		properties.addProperty(FEATURE_IDS, featureIds);
	}

	public static Feature getFeature(Properties properties) {
		List<Feature> features = getFeatures(properties);
		return features.isEmpty() ? null : features.get(0);
	}

	public static List<Feature> getFeatures(Properties properties) {
		String featureIds = properties.getProperty(FEATURE_IDS, "");
		List<Feature> features = Arrays.stream(featureIds.split(DELIMITER))
				.filter(id -> NumberUtils.isDigit(id))
				.map(id -> Long.valueOf(id))
				.map(id -> Screening.getEnvironment().getEntityManager().find(Feature.class, id))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		return features;
	}

	public static void setNormalization(Properties properties, String norm) {
		setNormalizations(properties, Arrays.asList(norm));
	}

	public static void setNormalizations(Properties properties, List<String> norms) {
		String normalizations = norms.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.joining(DELIMITER));
		properties.addProperty(NORMALIZATIONS, normalizations);
	}

	public  static String getNormalization(Properties properties) {
		List<String> norms = getNormalizations(properties);
		return norms.isEmpty() ? null : norms.get(0);
	}

	public static List<String> getNormalizations(Properties properties) {
		String normsString = properties.getProperty(NORMALIZATIONS, "");
		List<String> normalizations = Arrays.stream(normsString.split(DELIMITER))
				.collect(Collectors.toList());
		return normalizations;
	}

}
