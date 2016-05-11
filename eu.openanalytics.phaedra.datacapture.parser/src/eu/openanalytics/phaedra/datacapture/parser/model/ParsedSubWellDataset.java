package eu.openanalytics.phaedra.datacapture.parser.model;

import java.util.Arrays;


public class ParsedSubWellDataset {

	private String feature;

	private float[][] numericValues;
	private String[][] stringValues;
	
	public ParsedSubWellDataset(int size, boolean numeric) {
		this(size, 1, numeric);
	}
	
	public ParsedSubWellDataset(int size, int timepoints, boolean numeric) {
		if (numeric) {
			numericValues = new float[timepoints][size];
			// Absent values start as NaN, not zero!
			for (int i=0; i<numericValues.length; i++) {
				numericValues[i] = new float[size];
				Arrays.fill(numericValues[i], Float.NaN);
			}
		} else {
			stringValues = new String[timepoints][size];
			// Absent values start as null.
			for (int i=0; i<stringValues.length; i++) {
				stringValues[i] = new String[size];
			}
		}
	}
	
	public boolean isNumeric() {
		return stringValues == null;
	}

	public int getCellCount() {
		if (isNumeric()) {
			if (numericValues.length > 0) return numericValues[0].length;
		} else {
			if (stringValues.length > 0) return stringValues[0].length;
		}
		return 0;
	}
	
	public int getTimepoints() {
		if (isNumeric()) {
			return numericValues.length;
		} else {
			return stringValues.length;
		}
	}
	
	public String getFeature() {
		return feature;
	}

	public void setFeature(String feature) {
		this.feature = feature;
	}
	
	public String[] getStringValues() {
		// Assuming only one timepoint.
		return getStringValues(0);
	}
	
	public String[] getStringValues(int tp) {
		return stringValues[tp];
	}
	
	public float[] getNumericValues() {
		// Assuming only one timepoint.
		return getNumericValues(0);
	}
	
	public float[] getNumericValues(int tp) {
		return numericValues[tp];
	}
	
	public float[][] getAllNumericValues() {
		return numericValues;
	}

	public void addStringValue(int cell, String value) {
		addStringValue(cell, 0, value);
	}

	public void addStringValue(int cell, int tp, String value) {
		if (stringValues == null) throw new IllegalArgumentException("Cannot store value '" + value + "' in a numeric dataset.");
		stringValues[tp][cell] = value;
	}

	public void addNumericValue(int cell, float value) {
		addNumericValue(cell, 0, value);
	}
	
	public void addNumericValue(int cell, int tp, float value) {
		if (numericValues == null) throw new IllegalArgumentException("Cannot store numeric value '" + value + "' in a string dataset.");
		numericValues[tp][cell] = value;
	}
}
