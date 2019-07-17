package eu.openanalytics.phaedra.export.core.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.export.core.statistics.Statistics;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class QueryResult {

	public static final byte STRING_VALUE = 1;
	public static final byte BOOLEAN_VALUE = 2;
	public static final byte DOUBLE_VALUE = 3;
	public static final byte TIMESTAMP_VALUE = 4;

	private Query query;
	private Feature feature;
	
	private String[] colNames = new String[0];
	private byte[] colValueTypes = new byte[0];
	private int rowCount;
	
	private Statistics statistics;
	
	private ArrayList<Object[]> values = new ArrayList<>(); // (row)[col];
	private boolean finished;


	public QueryResult(Query query) {
		this.query = query;
	}
	
	public QueryResult() {
	}


	private Object checkValue(int col, Object value) {
		if (value == null) {
			return null;
		}
		switch (colValueTypes[col]) {
		case BOOLEAN_VALUE:
			if (value instanceof Boolean) {
				return value;
			}
			return null;
		case DOUBLE_VALUE:
			if (value instanceof Double) {
				return value;
			}
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}
			if (value instanceof String && NumberUtils.isDouble((String)value)) {
				return Double.parseDouble((String)value);
			}
			return null;
		case TIMESTAMP_VALUE:
			if (value instanceof Date) {
				return value;
			}
			return null;
		case STRING_VALUE:
		default:
			return value.toString();
		}
	}
	
	public void finish() {
		finished = true;
		values.trimToSize();
		for (Object[] rowValues : values) {
			for (int col=0; col<rowValues.length; col++) {
				rowValues[col] = checkValue(col, rowValues[col]);
			}
		}
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
	
	
	public int getColCount() {
		return colNames.length;
	}
	
	public void setColumnCount(int count) {
		this.colNames = new String[count];
		this.colValueTypes = new byte[count];
	}
	
	public String[] getColumnNames() {
		return colNames;
	}
	
	public String getColumnName(int col) {
		return colNames[col];
	}
	
	public int getColumnIndex(String name) {
		for (int i = 0; i < colNames.length; i++) {
			if (name.equals(colNames[i])) {
				return i;
			}
		}
		return -1;
	}
	
	public byte getColumnValueType(int col) {
		return colValueTypes[col];
	}
	
	public void setColumn(int col, byte type, String name) {
		this.colValueTypes[col] = type;
		this.colNames[col] = name;
	}
	
	public void setColumnName(int col, String name) {
		this.colNames[col] = name;
	}
	
	public void setColumnValueType(int col, byte type) {
		this.colValueTypes[col] = type;
	}
	
	public void setNumericColumn(int index) {
		setColumnValueType(index, DOUBLE_VALUE);
	}
	
	public boolean isColumnNumeric(int index) {
		return (colValueTypes[index] == DOUBLE_VALUE);
	}


	public int getRowCount() {
		return rowCount;
	}
	
	public void addRow(Object[] rowValues) {
		if (finished) throw new IllegalStateException("Cannot add row: finish has been called.");
		
		if (rowValues.length != colNames.length) {
			rowValues = Arrays.copyOf(rowValues, colNames.length);
		}
		values.add(rowValues);
		rowCount++;
	}

	/**
	 * Returns the value as object of the cell's value type.
	 * @return the object or <code>null</code> if NA
	 */
	public Object getValue(int row, int col) {
		return values.get(row)[col];
	}
	
	/**
	 * Returns the value as string.
	 * @return the string or <code>null</code> if NA
	 */
	public String getValueString(int row, int col) {
		Object value = getValue(row, col);
		return (value != null) ? value.toString() : null;
	}
	
	/**
	 * Returns the long value.
	 * If the cell is not numeric or NA, an exception is thrown!
	 * @return the primitiv value
	 */
	public long getLongValue(int row, int col) {
		return ((Number)getValue(row, col)).longValue();
	}
	
	public String getStringValue(int row, int col) {
		Object value = (colValueTypes[col] != DOUBLE_VALUE) ? getValue(row, col) : null;
		return (value != null) ? value.toString() : "";
	}
	
	public double getNumericValue(int row, int col) {
		Object value = (colValueTypes[col] == DOUBLE_VALUE) ? getValue(row, col) : null;
		return (value != null) ? ((Double) value).doubleValue() : Double.NaN;
	}
	
}
