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

	/**
	 * Filter plates that have calculation status 0 (TO DO)
	 */
	public static Predicate<Plate> CALCULATION_TODO = p -> p.getCalculationStatus() == 0;

	/**
	 * Filter plates that have validation status 0 (TO DO)
	 */
	public static Predicate<Plate> VALIDATION_TODO = p -> p.getValidationStatus() == 0;

	/**
	 * Filter plates that have approval status 0 (TO DO)
	 */
	public static Predicate<Plate> APPROVAL_TODO = p -> p.getApprovalStatus() == 0;

	/**
	 * Filter plates that have export status 0 (TO DO)
	 */
	public static Predicate<Plate> EXPORT_TODO = p -> p.getUploadStatus() == 0;

	/**
	 * Filter wells that are accepted (not rejected)
	 */
	public static Predicate<Well> ACCEPTED_WELLS_ONLY = w -> w.getStatus() >= 0;

	/**
	 * Create a filter for wells that match a given well type.
	 * 
	 * @param type The well type to filter on.
	 * @return A filter for wells that match the given well type.
	 */
	public static Predicate<Well> createWellTypeFilter(final String type) {
		return well -> {
			if (type == null) return true;
			return type.equals(well.getWellType());
		};
	}

	/**
	 * Create a filter for well feature values whose well matches a given well type.
	 * 
	 * @param type The well type to filter on.
	 * @return A filter for well feature values whose well matches the given well type.
	 */
	public static Predicate<FeatureValue> createWellTypeValueFilter(final String type) {
		return fv -> {
			if (fv.getWell() == null) return false;
			if (type == null) return true;
			return type.equals(fv.getWell().getWellType());
		};
	}

	/**
	 * Compare feature values by their normalized value.
	 */
	public static Comparator<FeatureValue> NORMALIZED_FEATURE_VALUE_SORTER = (fv1, fv2) -> {
		if (fv1 == null && fv2 == null) return 0;
		if (fv1 == null) return -1;
		if (fv2 == null) return 1;

		double value1 = Objects.equals(fv1.getFeature().getNormalization(), "NONE") ? Double.NaN : fv1.getNormalizedValue();
		double value2 = Objects.equals(fv2.getFeature().getNormalization(), "NONE") ? Double.NaN : fv2.getNormalizedValue();
		return Double.compare(value1, value2);
	};

	/**
	 * Compare feature values by their raw value.
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
	 * Compare wells by their compound number (compound type is not considered).
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
	 * Compare wells by:
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
	 * Compare wells by their well number.
	 */
	public static Comparator<Well> WELL_NR_SORTER = (w1, w2) -> {
		return Integer.valueOf(getWellNr(w1)).compareTo(getWellNr(w2));
	};

	/**
	 * Compare plates by:
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
	 * Compare plates by:
	 * 1. Experiment ID
	 * 2. Plate sequence
	 */
	public static Comparator<Plate> EXP_ID_PLATE_SEQ_SORTER = (p1, p2) -> {
		Experiment e1 = p1.getExperiment();
		Experiment e2 = p2.getExperiment();
		if (e1.getId() == e2.getId()) return p1.getSequence() - p2.getSequence();
		return (int)(e1.getId() - e2.getId());
	};

	/**
	 * Compare plates by their sequence nrs.
	 */
	public static Comparator<Plate> PLATE_SEQUENCE_SORTER = (p1, p2) -> p1.getSequence() - p2.getSequence();

	/**
	 * Compare plates by their primary IDs.
	 */
	public static Comparator<Plate> PLATE_ID_SORTER = (p1, p2) -> (int)(p1.getId() - p2.getId());

	/**
	 * Compare plates by their barcodes.
	 */
	public static Comparator<Plate> PLATE_BARCODE_SORTER = (p1, p2) -> {
		String b1 = p1.getBarcode();
		String b2 = p2.getBarcode();
		if (b1 == null && b2 == null) return 0;
		if (b1 == null) return -1;
		if (b2 == null) return 1;
		return b1.compareTo(b2);
	};

	/**
	 * Compare experiments by their names.
	 */
	public static Comparator<Experiment> EXPERIMENT_NAME_SORTER = (e1, e2) -> {
		if (e1 == null && e2 == null) return 0;
		if (e1 == null) return -1;
		if (e2 == null) return 1;
		return e1.getName().compareTo(e2.getName());
	};

	/**
	 * See {@link ProtocolUtils#isSample(String)}
	 */
	public static boolean isSample(Well well) {
		return ProtocolUtils.isSample(well.getWellType());
	}

	/**
	 * Get the well of a given plate at a given position.
	 * 
	 * @param plate The plate containing the well.
	 * @param row The well's row number, starting from 1.
	 * @param col The well's column number, starting from 1.
	 * @return The well at the given position, or null if the position is invalid for the given plate.
	 */
	public static Well getWell(Plate plate, int row, int col) {
		if (plate.getWells() == null) return null;
		for (Well well: plate.getWells()) {
			if (well.getRow() == row && well.getColumn() == col) return well;
		}
		return null;
	}

	/**
	 * Get the well of a given plate by its well number.
	 * 
	 * @param plate The plate containing the well.
	 * @param nr The well's number, starting from 1.
	 * @return The well with the given number, or null if the well number is invalid for the given plate.
	 */
	public static Well getWell(Plate plate, int nr) {
		if (plate.getWells() == null) return null;
		for (Well well: plate.getWells()) {
			int thisNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
			if (nr == thisNr) return well;
		}
		throw new IllegalArgumentException("Could not find well nr " + nr + " for plate \"" + plate.getBarcode() + "\"");
	}

	/**
	 * Get the well number of a well.
	 * 
	 * @param well The well whose number should be retrieved.
	 * @return The well number of the well.
	 */
	public static int getWellNr(Well well) {
		return NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
	}

	/**
	 * Get the coordinate of a well.
	 * See {@link NumberUtils#getWellCoordinate(int, int)}
	 * 
	 * @param well The well whose coordinate should be retrieved.
	 * @return The coordinate of the well.
	 */
	public static String getWellCoordinate(Well well) {
		return NumberUtils.getWellCoordinate(well.getRow(), well.getColumn());
	}

	/**
	 * Get the well count (i.e. the dimensions) of a plate.
	 * 
	 * @param plate The plate whose dimensions should be retrieved.
	 * @return The number of wells in the plate.
	 */
	public static int getWellCount(Plate plate) {
		return (plate.getColumns() * plate.getRows());
	}

	/**
	 * Check whether a well is a control well.
	 * See {@link ProtocolUtils#isControl(String)}.
	 * 
	 * @param well The well to check.
	 * @return True if the well is a control well.
	 */
	public static boolean isControl(Well well) {
		return ProtocolUtils.isControl(well.getWellType());
	}

	/**
	 * Get a list of all well types that occur in a given plate.
	 * 
	 * @param plate The plate to check.
	 * @return A list of all well types that occur in the given plate.
	 */
	public static List<String> getWellTypes(Plate plate) {
		List<String> types = new ArrayList<String>();
		for (Well well: plate.getWells()) {
			String type = well.getWellType();
			if (type != null) CollectionUtils.addUnique(types, type);
		}
		return types;
	}

	/**
	 * Get the compound in a plate that matches the given type and number.
	 * 
	 * @param plate The plate containing the compound.
	 * @param type The compound type.
	 * @param number The compound number.
	 * @return The matching compound, or null if the plate contains no such compound.
	 */
	public static Compound getCompound(Plate plate, String type, String number) {
		List<Compound> compounds = plate.getCompounds();
		for (Compound c: compounds) {
			if (c.getType().equals(type) && c.getNumber().equals(number)) {
				return c;
			}
		}
		return null;
	}

	/**
	 * From any platform object, retrieve the well features of the protocol
	 * class the object belongs to.
	 * 
	 * @param object The platform object, e.g. a well or plate.
	 * @return A list of well features of the object's protocol class.
	 */
	public static List<Feature> getFeatures(PlatformObject object) {
		return ProtocolUtils.getFeatures(object);
	}

	/**
	 * From any platform object, retrieve the subwell features of the protocol
	 * class the object belongs to.
	 * 
	 * @param object The platform object, e.g. a well or plate.
	 * @return A list of subwell features of the object's protocol class.
	 */
	public static List<SubWellFeature> getSubWellFeatures(PlatformObject object) {
		return ProtocolUtils.getSubWellFeatures(object);
	}

	/**
	 * From any platform object, retrieve the protocol class the object belongs to.
	 * 
	 * @param object The platform object, e.g. a well or plate.
	 * @return The object's protocol class.
	 */
	public static ProtocolClass getProtocolClass(PlatformObject object) {
		return ProtocolUtils.getProtocolClass(object);
	}

	/**
	 * Check if two platform objects belong to the same protocol class.
	 * 
	 * @param o1 Object 1, e.g. a well or plate.
	 * @param o2 Object 1, e.g. another well or plate.
	 * @return True if both objects belong to the same protocol class.
	 */
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
