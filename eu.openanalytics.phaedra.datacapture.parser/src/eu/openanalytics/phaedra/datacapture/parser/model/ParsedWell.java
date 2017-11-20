package eu.openanalytics.phaedra.datacapture.parser.model;

import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory representation of a parsed well.
 * A ParsedWell may contain:
 * <ul>
 * <li>A map of {@link ParsedFeature}s: the key is the name of the feature, the value is the text or numeric value for the feature</li>
 * <li>A map of {@link ParsedSubWellDataset}s: the key is the name of the feature, the value is an array of text or numeric values for the feature</li>
 * </ul>
 */
public class ParsedWell {

	private Map<String,ParsedFeature> features;
	private Map<String,ParsedSubWellDataset> subWellData;
	private Map<String, String> keywords;
	
	private int row;
	private int column;
	
	public ParsedWell() {
		features = new HashMap<>();
		subWellData =  new HashMap<>();
		keywords = new HashMap<>();
	}
	
	public ParsedFeature getFeature(String id) {
		return features.get(id);
	}
	
	public String[] getFeatureIds() {
		return features.keySet().toArray(new String[features.size()]);
	}
	
	public ParsedFeature[] getFeatures() {
		return features.values().toArray(new ParsedFeature[features.size()]);
	}
	
	public void addFeature(String id, ParsedFeature feature) {
		features.put(id, feature);
	}
	
	public Map<String, ParsedSubWellDataset> getSubWellData() {
		return subWellData;
	}
	
	public void addSubWellDataset(String feature, ParsedSubWellDataset dataset) {
		subWellData.put(feature, dataset);
	}
	
	public ParsedSubWellDataset getSubWellDataset(String feature) {
		return subWellData.get(feature);
	}
	
	public Map<String, String> getKeywords() {
		return keywords;
	}
	
	public void addKeyword(String keyName, String keyValue) {
		keywords.put(keyName, keyValue);
	}
	
	public String getKeyword(String keyName) {
		return keywords.get(keyName);
	}
	
	public int getRow() {
		return row;
	}
	
	public void setRow(int row) {
		this.row = row;
	}
	
	public int getColumn() {
		return column;
	}
	
	public void setColumn(int column) {
		this.column = column;
	}
}
