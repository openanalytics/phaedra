package eu.openanalytics.phaedra.datacapture.parser.util;

import eu.openanalytics.phaedra.datacapture.parser.model.ParsedFeature;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedPlate;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedSubWellDataset;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedWell;

public class ModelUtils {

	public static ParsedModel newModel() {
		return new ParsedModel();
	}

	public static ParsedPlate newPlate() {
		return new ParsedPlate();
	}

	public static ParsedPlate newPlate(ParsedModel model) {
		ParsedPlate plate = new ParsedPlate();
		model.addPlate(plate);
		return plate;
	}

	public static ParsedWell newWell() {
		return new ParsedWell();
	}

	public static ParsedWell newWell(ParsedPlate plate, int row, int col) {
		ParsedWell well = new ParsedWell();
		plate.addWell(row, col, well);
		return well;
	}

	public static ParsedFeature newFeature() {
		return new ParsedFeature();
	}

	public static ParsedFeature newFeature(String name, ParsedWell well) {
		ParsedFeature feature = new ParsedFeature();
		well.addFeature(name, feature);
		return feature;
	}

	public static ParsedSubWellDataset newSubWellDataset(int cells, boolean numeric) {
		return newSubWellDataset(cells, 1, numeric);
	}

	public static ParsedSubWellDataset newSubWellDataset(int cells, int timepoints, boolean numeric) {
		return new ParsedSubWellDataset(cells, timepoints, numeric);
	}
	
	public static ParsedSubWellDataset newSubWellDataset(int cells, boolean numeric, String name, ParsedWell well) {
		return newSubWellDataset(cells, 1, numeric, name, well);
	}
	
	public static ParsedSubWellDataset newSubWellDataset(int cells, int timepoints, boolean numeric, String name, ParsedWell well) {
		ParsedSubWellDataset ds = new ParsedSubWellDataset(cells, timepoints, numeric);
		ds.setFeature(name);
		well.addSubWellDataset(name, ds);
		return ds;
	}

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

	public static void setWellFloatValue(ParsedPlate plate, String featureName, int row, int column, Float numericValue) {
		ParsedWell well = plate.getWell(row, column);
		if (well == null) well = newWell(plate, row, column);
		ParsedFeature featureValue = well.getFeature(featureName);
		if (featureValue == null) featureValue = newFeature(featureName, well);
		featureValue.setNumericValue(numericValue);
	}

	public static void setWellStringValue(ParsedPlate plate, String featureName, int row, int column, String stringValue) {
		ParsedWell well = plate.getWell(row, column);
		if (well == null) well = newWell(plate, row, column);
		ParsedFeature featureValue = well.getFeature(featureName);
		if (featureValue == null) featureValue = newFeature(featureName, well);
		featureValue.setStringValue(stringValue);
	}

}
