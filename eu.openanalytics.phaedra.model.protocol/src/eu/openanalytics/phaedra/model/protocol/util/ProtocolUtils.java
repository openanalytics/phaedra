package eu.openanalytics.phaedra.model.protocol.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;

/**
 * A collection of utility methods related to protocols, protocol classes and features.
 */
public class ProtocolUtils {

	/**
	 * Map features to their names.
	 */
	public static Function<IFeature, String> FEATURE_NAMES = f -> f.getDisplayName();

	/**
	 * Filter numeric features.
	 */
	public static Predicate<IFeature> NUMERIC_FEATURES = f -> f.isNumeric();

	/**
	 * Filter key features.
	 */
	public static Predicate<IFeature> KEY_FEATURES = f -> f.isKey();
	
	/**
	 * Filter annotation features.
	 */
	public static Predicate<Feature> ANNOTATION_FEATURES = f -> f.isAnnotation();
	
	/**
	 * Map well types to their String representation.
	 */
	public static Function<WellType, String> WELLTYPE_CODES = t -> t.getCode();

	/**
	 * Sort features by their name.
	 */
	public static Comparator<IFeature> FEATURE_NAME_SORTER = (o1, o2) -> {
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
	};

	/**
	 * Sort features by their calculation sequence.
	 */
	public static Comparator<Feature> FEATURE_CALC_SEQUENCE = (o1, o2) -> {
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		return Integer.compare(o1.getCalculationSequence(), o2.getCalculationSequence());
	};

	/**
	 * Sort features by their group name.
	 */
	public static Comparator<FeatureGroup> FEATURE_GROUP_NAME_SORTER = (o1, o2) -> {
		if (o1 == o2) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		return o1.getName().compareTo(o2.getName());
	};

	/**
	 * Sort protocol class by their name.
	 */
	public static Comparator<ProtocolClass> PROTOCOLCLASS_NAME_SORTER = (p1, p2) -> {
		if (p1 == null && p2 == null) return 0;
		if (p1 == null) return -1;
		if (p2 == null) return 1;
		return p1.getName().compareTo(p2.getName());
	};

	/**
	 * Sort protocols by their name.
	 */
	public static Comparator<Protocol> PROTOCOL_NAME_SORTER = (p1, p2) -> {
		if (p1 == null && p2 == null) return 0;
		if (p1 == null) return -1;
		if (p2 == null) return 1;
		return p1.getName().compareTo(p2.getName());
	};

	/**
	 * Get a list of all well features of given platform object's protocol class.
	 * 
	 * @param object A platform object, e.g. a well or plate.
	 * @return A list of well features of the parent protocol class.
	 */
	public static List<Feature> getFeatures(PlatformObject object) {
		ProtocolClass pc = getProtocolClass(object);
		if (pc == null) return new ArrayList<>();
		return ProtocolService.streamableList(pc.getFeatures()).stream()
				.sorted(FEATURE_NAME_SORTER)
				.collect(Collectors.toList());
	}
	
	/**
	 * Get a list of all subwell features of given platform object's protocol class.
	 * 
	 * @param object A platform object, e.g. a well or plate.
	 * @return A list of subwell features of the parent protocol class.
	 */
	public static List<SubWellFeature> getSubWellFeatures(PlatformObject object) {
		ProtocolClass pc = getProtocolClass(object);
		if (pc == null) return new ArrayList<>();
		return ProtocolService.streamableList(pc.getSubWellFeatures()).stream()
				.sorted(FEATURE_NAME_SORTER)
				.collect(Collectors.toList());
	}

	/**
	 * Look up a well feature by its name.
	 * 
	 * @param name The name of the feature.
	 * @param pClass The protocol class to search in.
	 * @return The matching feature, or null if no match was found.
	 */
	public static Feature getFeatureByName(String name, ProtocolClass pClass) {
		List<Feature> features = pClass.getFeatures();
		for (Feature f: features) {
			if (f.getName().equalsIgnoreCase(name)) return f;
			if (f.getDisplayName().equalsIgnoreCase(name)) return f;
		}
		return null;
	}

	/**
	 * Look up a subwell feature by its name.
	 * 
	 * @param name The name of the feature.
	 * @param pClass The protocol class to search in.
	 * @return The matching feature, or null if no match was found.
	 */
	public static SubWellFeature getSubWellFeatureByName(String name, ProtocolClass pClass) {
		List<SubWellFeature> features = pClass.getSubWellFeatures();
		for (SubWellFeature f: features) {
			if (f.getName().equalsIgnoreCase(name)) return f;
			if (f.getDisplayName().equalsIgnoreCase(name)) return f;
		}
		return null;
	}

	/**
	 * Get the parent protocol class of a platform object.
	 * <p>
	 * Note: this method is synchronized because AdapterManager is not threadsafe,
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=414082
	 * </p>
	 * @param object A platform object, e.g. a well or plate.
	 * @return The parent protocol class.
	 */
	public static synchronized ProtocolClass getProtocolClass(PlatformObject object) {
		// Synchronized, because AdapterManager is not threadsafe, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=414082
		if (object == null) return null;
		return (ProtocolClass) object.getAdapter(ProtocolClass.class);
	}

	/**
	 * Get the well type that represents low control wells for the given feature.
	 * 
	 * @param f The feature to get the low control type for.
	 * @return The low control well type.
	 */
	public static String getLowType(Feature f) {
		String code = f.getLowWellTypeCode();
		if (code == null) code = f.getProtocolClass().getLowWellTypeCode();
		return code;
	}

	/**
	 * Get the well type that represents high control wells for the given feature.
	 * 
	 * @param f The feature to get the high control type for.
	 * @return The high control well type.
	 */
	public static String getHighType(Feature f) {
		String code = f.getHighWellTypeCode();
		if (code == null) code = f.getProtocolClass().getHighWellTypeCode();
		return code;
	}

	/**
	 * Check if the given feature has a normalization method configured.
	 * 
	 * @param feature The feature to check.
	 * @return True if the feature has a normalization method configured.
	 */
	public static boolean isNormalized(Feature feature) {
		String normalization = feature.getNormalization();
		return normalization != null && !normalization.equals("NONE");
	}

	/**
	 * Check if the given well type represents SAMPLE wells.
	 * 
	 * @param wellType The well type to check.
	 * @return True if the well type represents SAMPLE wells.
	 */
	public static boolean isSample(String wellType) {
		return WellType.SAMPLE.equals(wellType);
	}

	/**
	 * Check if the given well type represents control wells.
	 * Currently, it is assumed that any well type that is not
	 * SAMPLE or EMPTY, is a control type.
	 * 
	 * @param wellType The well type to check.
	 * @return True if the given well type represents a control well.
	 */
	public static boolean isControl(String wellType) {
		if (wellType == null) return false;
		if (wellType.equals(WellType.SAMPLE)) return false;
		if (wellType.equals(WellType.EMPTY)) return false;
		if (wellType.equals(WellType.NA)) return false;
		return true;
	}

	/**
	 * Get the default RGB color for the given well type.
	 * 
	 * @param wellType The well type to get a color for.
	 * @return A color matching the given well type.
	 */
	public static RGB getWellTypeRGB(String wellType) {
		if (wellType == null || wellType.equals(WellType.EMPTY)) {
			return new RGB(150,150,150);
		} else if (wellType.equals(WellType.SAMPLE)) {
			return new RGB(80,80,200);
		} else if (wellType.equals(WellType.LC)) {
			return new RGB(200,0,0);
		} else if (wellType.equals(WellType.HC)) {
			return new RGB(0,200,0);
		}
		return new RGB(150,150,0);
	}
	
	/**
	 * Get a gradient of the SAMPLE type color (see {@link ProtocolUtils#getWellTypeRGB(String)}
	 * according to the given concentration.
	 * 
	 * @param conc The concentration to create a gradient for.
	 * @return A gradient of the SAMPLE well type color.
	 */
	public static RGB getWellConcRGB(double conc) {
		RGB rgb = getWellTypeRGB("SAMPLE");
		if (Double.isNaN(conc) || conc == 0.0) return rgb;

		// The lower the conc, the higher the percentage.
		double logConc = -Math.log10(conc);
		double percent = (logConc - 4) / 7; 

		rgb.red = (int) (rgb.red + ((255 - rgb.red) * percent));
		rgb.green = (int) (rgb.green + ((255 - rgb.green) * percent));
		rgb.blue = (int) (rgb.blue + ((255 - rgb.blue) * percent));

		rgb.red = Math.max(0, Math.min(255, rgb.red));
		rgb.green = Math.max(0, Math.min(255, rgb.green));
		rgb.blue = Math.max(0, Math.min(255, rgb.blue));
		
		return rgb;
	}

	public static boolean[] getEnabledChannels(ImageSettings settings, boolean detailedView) {
		List<ImageChannel> channels = settings.getImageChannels();
		boolean[] enabledChannels = new boolean[channels.size()];
		for (int i = 0; i < enabledChannels.length; i++) {
			enabledChannels[i] = detailedView ? channels.get(i).isShowInWellView() : channels.get(i).isShowInPlateView();
		}
		return enabledChannels;
	}
}
