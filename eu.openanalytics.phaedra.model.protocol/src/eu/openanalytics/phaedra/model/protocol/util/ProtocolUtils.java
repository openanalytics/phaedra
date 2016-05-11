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
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;

/**
 * A collection of general utility methods related to protocols, protocol classes and features.
 */
public class ProtocolUtils {

	public static Function<IFeature, String> FEATURE_NAMES = f -> f.getDisplayName();

	public static Predicate<IFeature> NUMERIC_FEATURES = f -> f.isNumeric();

	public static Predicate<IFeature> KEY_FEATURES = f -> f.isKey();
	
	public static Predicate<Feature> ANNOTATION_FEATURES = f -> f.isAnnotation();
	
	public static Predicate<Feature> FEATURES_WITH_CURVES = f -> {
		String kind = f.getCurveSettings().get("KIND");
		return (kind != null && !kind.isEmpty() && !kind.equals("NONE"));
	};

	public static Function<WellType, String> WELLTYPE_CODES = t -> t.getCode();

	public static Comparator<IFeature> FEATURE_NAME_SORTER = (o1, o2) -> {
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
	};

	public static Comparator<Feature> FEATURE_CALC_SEQUENCE = (o1, o2) -> {
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		return Integer.compare(o1.getCalculationSequence(), o2.getCalculationSequence());
	};

	public static Comparator<FeatureGroup> FEATURE_GROUP_NAME_SORTER = (o1, o2) -> {
		if (o1 == o2) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		return o1.getName().compareTo(o2.getName());
	};

	public static Comparator<ProtocolClass> PROTOCOLCLASS_NAME_SORTER = (p1, p2) -> {
		if (p1 == null && p2 == null) return 0;
		if (p1 == null) return -1;
		if (p2 == null) return 1;
		return p1.getName().compareTo(p2.getName());
	};

	public static Comparator<Protocol> PROTOCOL_NAME_SORTER = (p1, p2) -> {
		if (p1 == null && p2 == null) return 0;
		if (p1 == null) return -1;
		if (p2 == null) return 1;
		return p1.getName().compareTo(p2.getName());
	};

	public static List<Feature> getFeatures(PlatformObject object) {
		ProtocolClass pc = getProtocolClass(object);
		if (pc == null) return new ArrayList<>();
		return ProtocolService.streamableList(pc.getFeatures()).stream()
				.sorted(FEATURE_NAME_SORTER)
				.collect(Collectors.toList());
	}

	public static List<SubWellFeature> getSubWellFeatures(PlatformObject object) {
		ProtocolClass pc = getProtocolClass(object);
		if (pc == null) return new ArrayList<>();
		return ProtocolService.streamableList(pc.getSubWellFeatures()).stream()
				.sorted(FEATURE_NAME_SORTER)
				.collect(Collectors.toList());
	}

	public static Feature getFeatureByName(String name, ProtocolClass pClass) {
		List<Feature> features = pClass.getFeatures();
		for (Feature f: features) {
			if (f.getName().equalsIgnoreCase(name)) return f;
			if (f.getDisplayName().equalsIgnoreCase(name)) return f;
		}
		return null;
	}

	public static SubWellFeature getSubWellFeatureByName(String name, ProtocolClass pClass) {
		List<SubWellFeature> features = pClass.getSubWellFeatures();
		for (SubWellFeature f: features) {
			if (f.getName().equalsIgnoreCase(name)) return f;
			if (f.getDisplayName().equalsIgnoreCase(name)) return f;
		}
		return null;
	}

	public static synchronized ProtocolClass getProtocolClass(PlatformObject object) {
		// Synchronized, because AdapterManager is not threadsafe, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=414082
		if (object == null) return null;
		return (ProtocolClass) object.getAdapter(ProtocolClass.class);
	}

	public static String getLowType(Feature f) {
		String code = f.getLowWellTypeCode();
		if (code == null) code = f.getProtocolClass().getLowWellTypeCode();
		return code;
	}

	public static String getHighType(Feature f) {
		String code = f.getHighWellTypeCode();
		if (code == null) code = f.getProtocolClass().getHighWellTypeCode();
		return code;
	}

	public static boolean isNormalized(Feature feature) {
		String normalization = feature.getNormalization();
		return normalization != null && !normalization.equals("NONE");
	}

	public static boolean isControl(String wellType) {
		if (wellType == null) return false;
		if (wellType.equals("SAMPLE")) return false;
		if (wellType.equals("EMPTY")) return false;
		if (wellType.equals("N/A")) return false;
		return true;
	}

	public static RGB getWellTypeRGB(String wellType) {
		if (wellType == null || wellType.equals("EMPTY")) {
			return new RGB(150,150,150);
		} else if (wellType.equals("SAMPLE")) {
			return new RGB(80,80,200);
		} else if (wellType.equals("LC")) {
			return new RGB(200,0,0);
		} else if (wellType.equals("HC")) {
			return new RGB(0,200,0);
		}
		return new RGB(150,150,0);
	}
	
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

}
