package eu.openanalytics.phaedra.export.core.query;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.export.core.statistics.Statistics;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class QueryResult {

	private Query query;
	private Feature feature;
	
	private String[] columns;
	private BitSet numericColumns;
	
	private String[][] stringValues;
	private double[][] numericValues;
	private int[] stringColumnIndices;
	private int[] numericColumnIndices;
	private int rowCount;
	
	private Statistics statistics;
	
	private Map<String,List<Object>> tempValues;
	
	public QueryResult(Query query) {
		this.query = query;
		this.tempValues = new HashMap<>();
		this.numericColumns = new BitSet();
	}

	public void addRow(Object[] data) {
		if (tempValues == null) throw new IllegalStateException("Cannot add row: finish has been called.");
		
		for (int i=0; i<data.length; i++) {
			String colName = columns[i];
			List<Object> values = tempValues.get(colName);
			if (values == null) {
				values = new ArrayList<>();
				tempValues.put(colName, values);
			}
			values.add(data[i]);
		}
		rowCount++;
	}
	
	public void finish() {
		int numericColumnCount = numericColumns.cardinality();
		int stringColumnCount = columns.length - numericColumnCount;
		stringValues = new String[stringColumnCount][rowCount];
		numericValues = new double[numericColumnCount][rowCount];
		stringColumnIndices = new int[columns.length];
		numericColumnIndices = new int[columns.length];
		int numericColumnIndex = 0;
		int stringColumnIndex = 0;
		
		for (int c=0; c<columns.length; c++) {
			boolean num = numericColumns.get(c);
			int colIndex = num ? numericColumnIndex++ : stringColumnIndex++;
			numericColumnIndices[c] = num ? colIndex : -1;
			stringColumnIndices[c] = num ? -1 : colIndex;
			List<Object> temp = tempValues.get(columns[c]);
			
			for (int r=0; r<rowCount; r++) {
				Object v = temp.get(r);
				if (num) {
					numericValues[colIndex][r] = Double.NaN;
					if (v instanceof Number) {
						numericValues[colIndex][r] = ((Number)v).doubleValue();
					} else if (v instanceof String && NumberUtils.isDouble((String)v)) {
						numericValues[colIndex][r] = Double.parseDouble((String)v);
					}
				} else {
					stringValues[colIndex][r] = (v == null) ? "" : v.toString();
				}
			}
		}
		
		tempValues.clear();
		tempValues = null;
	}
	
	public Query getQuery() {
		return query;
	}
	
	public void setFeature(Feature feature) {
		this.feature = feature;
	}
	
	public Feature getFeature() {
		return feature;
	}

	public Statistics getStatistics() {
		return statistics;
	}
	
	public void setStatistics(Statistics statistics) {
		this.statistics = statistics;
	}
	
	public String[] getColumns() {
		return columns;
	}
	
	public void setColumns(String[] columns) {
		this.columns = columns;
	}
	
	public void setNumericColumn(int index) {
		this.numericColumns.set(index);
	}
	
	public boolean isColumnNumeric(int index) {
		return numericColumns.get(index);
	}
	
	public int getRowCount() {
		return rowCount;
	}
	
	public String getStringValue(int row, int col) {
		int colIndex = stringColumnIndices[col];
		return (colIndex == -1) ? "" : stringValues[colIndex][row];
	}
	
	public double getNumericValue(int row, int col) {
		int colIndex = numericColumnIndices[col];
		return (colIndex == -1) ? Double.NaN : numericValues[colIndex][row];
	}
}
