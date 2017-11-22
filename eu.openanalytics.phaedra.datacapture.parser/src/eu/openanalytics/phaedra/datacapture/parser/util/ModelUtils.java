package eu.openanalytics.phaedra.datacapture.parser.util;

import eu.openanalytics.phaedra.datacapture.parser.model.ParsedFeature;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedPlate;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedSubWellDataset;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedWell;

/**
 * A collection of utilities for creating {@link ParsedModel}s during data capture.
 */
public class ModelUtils {

	/**
	 * Create a new, blank model without any plates.
	 * 
	 * @return A new model.
	 */
	public static ParsedModel newModel() {
		return new ParsedModel();
	}

	/**
	 * Create a new, blank plate without any wells and not attached to any model.
	 * 
	 * @return A new plate.
	 */
	public static ParsedPlate newPlate() {
		return new ParsedPlate();
	}

	/**
	 * Create a new, blank plate without any wells, attached to the given model.
	 * 
	 * @param model The model to attach the plate to.
	 * @return A new plate.
	 */
	public static ParsedPlate newPlate(ParsedModel model) {
		ParsedPlate plate = new ParsedPlate();
		model.addPlate(plate);
		return plate;
	}

	/**
	 * Create a new, blank well not attached to any plate.
	 * 
	 * @return A new well.
	 */
	public static ParsedWell newWell() {
		return new ParsedWell();
	}

	/**
	 * Createa a new well in the given plate at the given position.
	 * 
	 * @param plate The plate to add the well to.
	 * @param row The row of the well.
	 * @param col The column of the well.
	 * @return A new well.
	 */
	public static ParsedWell newWell(ParsedPlate plate, int row, int col) {
		ParsedWell well = new ParsedWell();
		plate.addWell(row, col, well);
		return well;
	}

	/**
	 * Create a new parsed well feature value, not attached to any well.
	 * 
	 * @return A new well feature value.
	 */
	public static ParsedFeature newFeature() {
		return new ParsedFeature();
	}

	/**
	 * Create a new parsed well feature value in the given well.
	 * 
	 * @param name The name of the feature.
	 * @param well The well containing the feature value.
	 * @return A new well feature value.
	 */
	public static ParsedFeature newFeature(String name, ParsedWell well) {
		ParsedFeature feature = new ParsedFeature();
		well.addFeature(name, feature);
		return feature;
	}

	/**
	 * Create a new subwell dataset of the given size, not
	 * attached to any well.
	 * 
	 * @param cells The size of the dataset.
	 * @param numeric True for a numeric dataset, false for a String dataset.
	 * @return A new dataset.
	 */
	public static ParsedSubWellDataset newSubWellDataset(int cells, boolean numeric) {
		return new ParsedSubWellDataset(cells, 1, numeric);
	}
	
	/**
	 * Create a new subwell dataset of the given size, for the given feature and well.
	 * 
	 * @param cells The size of the dataset.
	 * @param numeric True for a numeric dataset, false for a String dataset.
	 * @param name The name of the subwell feature.
	 * @param well The well to contain the dataset.
	 * @return A new dataset.
	 */
	public static ParsedSubWellDataset newSubWellDataset(int cells, boolean numeric, String name, ParsedWell well) {
		ParsedSubWellDataset ds = new ParsedSubWellDataset(cells, 1, numeric);
		ds.setFeature(name);
		well.addSubWellDataset(name, ds);
		return ds;
	}
	
	/**
	 * Create a new plate in the given model, with the given dimensions.
	 * 
	 * @param model The model to contain the new plate.
	 * @param rows The rows of the plate.
	 * @param columns The columns of the plate.
	 * @return A new plate.
	 */
	public static ParsedPlate createPlate(ParsedModel model, int rows, int columns) {
		ParsedPlate plate = new ParsedPlate();
		model.addPlate(plate);
		plate.setRows(rows);
		plate.setColumns(columns);
		
		for (int row = 1; row <= rows; row++) {
			for (int column = 1; column <= columns; column++) {
				ParsedWell well = new ParsedWell();
				plate.addWell(row, column, well);
			}
		}

		return plate;
	}

	/**
	 * Set the value of the given numeric well feature.
	 * 
	 * @param plate The parent plate.
	 * @param featureName The name of the feature to set a value for.
	 * @param row The row of the well to set a value for.
	 * @param column The column of the well to set a value for.
	 * @param numericValue The value to set.
	 */
	public static void setWellFloatValue(ParsedPlate plate, String featureName, int row, int column, Float numericValue) {
		ParsedWell well = plate.getWell(row, column);
		if (well == null) well = newWell(plate, row, column);
		ParsedFeature featureValue = well.getFeature(featureName);
		if (featureValue == null) featureValue = newFeature(featureName, well);
		featureValue.setNumericValue(numericValue);
	}

	/**
	 * Set the value of the given string well feature.
	 * 
	 * @param plate The parent plate.
	 * @param featureName The name of the feature to set a value for.
	 * @param row The row of the well to set a value for.
	 * @param column The column of the well to set a value for.
	 * @param stringValue The value to set.
	 */
	public static void setWellStringValue(ParsedPlate plate, String featureName, int row, int column, String stringValue) {
		ParsedWell well = plate.getWell(row, column);
		if (well == null) well = newWell(plate, row, column);
		ParsedFeature featureValue = well.getFeature(featureName);
		if (featureValue == null) featureValue = newFeature(featureName, well);
		featureValue.setStringValue(stringValue);
	}

}
