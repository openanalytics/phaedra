package eu.openanalytics.phaedra.model.plate.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

/**
 * A collection of general utility methods related to plates, wells and compounds.
 */
public class PlateUtils {

	public static Predicate<Plate> CALCULATION_TODO = p -> p.getCalculationStatus() == 0;

	public static Predicate<Plate> VALIDATION_TODO = p -> p.getValidationStatus() == 0;

	public static Predicate<Plate> APPROVAL_TODO = p -> p.getApprovalStatus() == 0;

	public static Predicate<Plate> EXPORT_TODO = p -> p.getUploadStatus() == 0;

	public static Predicate<Well> ACCEPTED_WELLS_ONLY = w -> w.getStatus() >= 0;

	public static Predicate<Well> createWellTypeFilter(final String type) {
		return well -> {
			if (type == null) return true;
			return type.equals(well.getWellType());
		};
	}

	public static Predicate<FeatureValue> createWellTypeValueFilter(final String type) {
		return fv -> {
			if (fv.getWell() == null) return false;
			if (type == null) return true;
			return type.equals(fv.getWell().getWellType());
		};
	}

	/**
	 * Compares two feature values by their normalized value.
	 */
	public static Comparator<FeatureValue> NORMALIZED_FEATURE_VALUE_SORTER = (fv1, fv2) -> {
		if (fv1 == null && fv2 == null) return 0;
		if (fv1 == null) return -1;
		if (fv2 == null) return 1;

		double value1 = Objects.equals(fv1.getFeature(), "NONE") ? Double.NaN : fv1.getNormalizedValue();
		double value2 = Objects.equals(fv2.getFeature(), "NONE") ? Double.NaN : fv2.getNormalizedValue();
		return Double.compare(value1, value2);
	};

	/**
	 * Compares two feature values by their raw value.
	 */
	public static Comparator<FeatureValue> RAW_FEATURE_VALUE_SORTER = (fv1, fv2) -> {
		if (fv1 == null && fv2 == null) return 0;
		if (fv1 == null) return -1;
		if (fv2 == null) return 1;

		Object value1 = fv1.getFeature().isNumeric() ? fv1.getRawNumericValue() : fv1.getRawStringValue();
		Object value2 = fv2.getFeature().isNumeric() ? fv2.getRawNumericValue() : fv2.getRawStringValue();
		if (value1 == null && value2 == null) return 0;
		if (value1 == null) return -1;
		if (value2 == null) return 1;

		if (value1 instanceof Double && value2 instanceof String) return -1;
		if (value1 instanceof String && value2 instanceof Double) return 1;
		if (value1 instanceof Double && value2 instanceof Double) return Double.compare((Double) value1, (Double) value2);
		return ((String) value1).compareTo((String) value2);
	};

	/**
	 * Compares two wells by their compound number (compound type is not considered).
	 */
	public static Comparator<Well> WELL_COMPOUND_NR_SORTER = (w1, w2) -> {
		if (w1 == null && w2 == null) return 0;
		Compound c1 = w1 == null ? null : w1.getCompound();
		Compound c2 = w2 == null ? null : w2.getCompound();
		if (c1 == null && c2 == null) return 0;
		if (c1 == null) return -1;
		if (c2 == null) return 1;
		return c1.getNumber().compareTo(c2.getNumber());
	};

	/**
	 * Compares two wells by:
	 * 1. Experiment name
	 * 2. Plate barcode
	 * 3. Well number
	 */
	public static Comparator<Well> WELL_EXP_NAME_PLATE_BARCODE_WELL_NR_SORTER = (w1, w2) -> {
		int c;
		c = w1.getPlate().getExperiment().getName().compareTo(w2.getPlate().getExperiment().getName());
		if (c == 0)
			c = w1.getPlate().getBarcode().compareTo(w2.getPlate().getBarcode());
		if (c == 0)
			c = Integer.valueOf(getWellNr(w1)).compareTo(getWellNr(w2));
		return c;
	};

	/**
	 * Compares two wells by their well number.
	 */
	public static Comparator<Well> WELL_NR_SORTER = (w1, w2) -> {
		return Integer.valueOf(getWellNr(w1)).compareTo(getWellNr(w2));
	};

	/**
	 * Compares two plates by:
	 * 1. Experiment name
	 * 2. Plate barcode
	 */
	public static Comparator<Plate> PLATE_EXP_NAME_PLATE_BARCODE_SORTER = (p1, p2) -> {
		int c;
		c = p1.getExperiment().getName().compareTo(p2.getExperiment().getName());
		if (c == 0)
			c = p1.getBarcode().compareTo(p2.getBarcode());
		return c;
	};

	/**
	 * Compares two plates by:
	 * 1. Experiment ID
	 * 2. Plate sequence
	 */
	public static Comparator<Plate> EXP_ID_PLATE_SEQ_SORTER = (p1, p2) -> {
		Experiment e1 = p1.getExperiment();
		Experiment e2 = p2.getExperiment();
		if (e1.getId() == e2.getId()) return p1.getSequence() - p2.getSequence();
		return (int)(e1.getId() - e2.getId());
	};

	public static Comparator<Plate> PLATE_SEQUENCE_SORTER = (p1, p2) -> p1.getSequence() - p2.getSequence();

	public static Comparator<Plate> PLATE_ID_SORTER = (p1, p2) -> (int)(p1.getId() - p2.getId());

	public static Comparator<Plate> PLATE_BARCODE_SORTER = (p1, p2) -> {
		String b1 = p1.getBarcode();
		String b2 = p2.getBarcode();
		if (b1 == null && b2 == null) return 0;
		if (b1 == null) return -1;
		if (b2 == null) return 1;
		return b1.compareTo(b2);
	};

	public static Comparator<Experiment> EXPERIMENT_NAME_SORTER = (e1, e2) -> {
		if (e1 == null && e2 == null) return 0;
		if (e1 == null) return -1;
		if (e2 == null) return 1;
		return e1.getName().compareTo(e2.getName());
	};

	public static boolean isSample(Well well) {
		return "SAMPLE".equals(well.getWellType());
	}

	public static Well getWell(Plate plate, int row, int col) {
		if (plate.getWells() == null) return null;
		for (Well well: plate.getWells()) {
			if (well.getRow() == row && well.getColumn() == col) return well;
		}
		return null;
	}

	public static Well getWell(Plate plate, int nr) {
		if (plate.getWells() == null) return null;
		for (Well well: plate.getWells()) {
			int thisNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
			if (nr == thisNr) return well;
		}
		throw new IllegalArgumentException("Could not find well nr " + nr + " for plate \"" + plate.getBarcode() + "\"");
	}

	public static int getWellNr(Well well) {
		return NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
	}

	public static String getWellCoordinate(Well well) {
		return NumberUtils.getWellCoordinate(well.getRow(), well.getColumn());
	}

	public static int getWellCount(Plate plate) {
		return (plate.getColumns() * plate.getRows());
	}

	public static boolean isControl(Well well) {
		return ProtocolUtils.isControl(well.getWellType());
	}

	public static List<String> getWellTypes(Plate plate) {
		List<String> types = new ArrayList<String>();
		for (Well well: plate.getWells()) {
			String type = well.getWellType();
			if (type != null) CollectionUtils.addUnique(types, type);
		}
		return types;
	}

	public static Compound getCompound(Plate plate, String type, String number) {
		List<Compound> compounds = plate.getCompounds();
		for (Compound c: compounds) {
			if (c.getType().equals(type) && c.getNumber().equals(number)) {
				return c;
			}
		}
		return null;
	}

	public static List<Feature> getFeatures(PlatformObject object) {
		return ProtocolUtils.getFeatures(object);
	}

	public static List<SubWellFeature> getSubWellFeatures(PlatformObject object) {
		return ProtocolUtils.getSubWellFeatures(object);
	}

	public static ProtocolClass getProtocolClass(PlatformObject object) {
		return ProtocolUtils.getProtocolClass(object);
	}

	public static boolean isSameProtocolClass(PlatformObject o1, PlatformObject o2) {
		if (o1 == null && o2 == null) return true;
		if (o1 == null) return false;
		ProtocolClass pc1 = getProtocolClass(o1);
		ProtocolClass pc2 = getProtocolClass(o2);
		if (pc1 == null && pc2 == null) return true;
		if (pc1 == null) return false;
		return pc1.equals(pc2);
	}
}
