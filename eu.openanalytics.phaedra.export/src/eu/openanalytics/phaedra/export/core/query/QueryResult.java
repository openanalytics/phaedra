package eu.openanalytics.phaedra.export.core.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.export.core.ExportInfo;
import eu.openanalytics.phaedra.export.core.statistics.Statistics;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class QueryResult {
	
	
	public static class Column {
		
		private String name;
		private int index;
		private DataDescription dataDescription;
//		private Object values;
		
		private Column(final String name, final int index,
				final DataDescription dataDescription) {
			this.name = name;
			this.index = index;
			this.dataDescription = dataDescription;
		}
		
		public final String getName() {
			return this.name;
		}
		
		public void setName(final String name) {
//			if (QueryResult.this.finished) throw new IllegalStateException();
			this.name = name;
		}
		
		public final int getIndex() {
			return this.index;
		}
		
		public final DataDescription getDataType() {
			return this.dataDescription;
		}
		
		@Override
		public String toString() {
			return this.name;
		}
		
	}
	
	
	private Query query;
	private Feature feature;
	
	private final ArrayList<Column> columns = new ArrayList<>();
	private int rowCount;
	
	private Statistics statistics;
	
	private ArrayList<Object[]> values = new ArrayList<>(); // (row)[col];
	private boolean finished;
	
	private List<ExportInfo> additionalInfos = new ArrayList<>();
	
	
	public QueryResult(Query query) {
		this.query = query;
	}
	
	public QueryResult() {
	}
	
	
	private Object checkValue(Column column, Object value) {
		if (value == null) {
			return null;
		}
		switch (column.dataDescription.getDataType()) {
		case Boolean:
			if (value instanceof Boolean) {
				return value;
			}
			return null;
		case Integer:
		case Real:
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
		case DateTime:
			if (value instanceof Date) {
				return value;
			}
			return null;
		default:
			return value.toString();
		}
	}
	
	public void finish() {
		finished = true;
		int colCount = this.columns.size();
		
		values.trimToSize();
		for (Object[] rowValues : values) {
			for (int col = 0; col < colCount; col++) {
				Column column = columns.get(col);
				rowValues[col] = checkValue(column, rowValues[col]);
			}
		}
	}
	
	public Query getQuery() {
		return query;
	}
	
	public void addInfo(ExportInfo info) {
		this.additionalInfos.add(info);
	}
	
	public List<ExportInfo> getAdditionalInfos() {
		return this.additionalInfos;
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
	
	
	public int getColumnCount() {
		return this.columns.size();
	}
	
	public List<Column> getColumns() {
		return this.columns;
	}
	
	public Column getColumn(final int col) {
		return this.columns.get(col);
	}
	
	public Column getColumn(final String name) {
		for (final Column column : this.columns) {
			if (name.equals(column.getName())) {
				return column;
			}
		}
		return null;
	}
	
	public int getColumnIndex(final String name) {
		final Column column = getColumn(name);
		return (column != null) ? column.getIndex() : -1;
	}
	
	
	public void addColumn(DataDescription dataDescription, String name) {
		final Column column = new Column(name, this.columns.size(), dataDescription);
		this.columns.add(column);
	}
	
	public void addColumn(DataDescription dataDescription) {
		addColumn(dataDescription, dataDescription.getName());
	}
	
	
	public int getRowCount() {
		return rowCount;
	}
	
	public void addRow(Object[] rowValues) {
		if (finished) throw new IllegalStateException("Cannot add row: finish has been called.");
		
		if (rowValues.length != columns.size()) {
			rowValues = Arrays.copyOf(rowValues, columns.size());
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
	
	
	public boolean isColumnNumeric(int index) {
		Column column = this.columns.get(index);
		switch (column.dataDescription.getDataType()) {
		case Integer:
		case Real:
			return true;
		default:
			return false;
		}
	}
	
	public String getStringValue(int row, int col) {
		Object value = (!isColumnNumeric(col)) ? getValue(row, col) : null;
		return (value != null) ? value.toString() : "";
	}
	
	public double getNumericValue(int row, int col) {
		Object value = (isColumnNumeric(col)) ? getValue(row, col) : null;
		return (value != null) ? ((Double) value).doubleValue() : Double.NaN;
	}
	
}
