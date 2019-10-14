package eu.openanalytics.phaedra.export.core.util;

import java.sql.JDBCType;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.export.core.ExportException;
import eu.openanalytics.phaedra.export.core.query.Query;
import eu.openanalytics.phaedra.export.core.query.QueryExecutor;
import eu.openanalytics.phaedra.export.core.query.QueryResult;

public class SQLUtils {

	private final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
	
	public static String generateSQLDate(Date date) {
		String dateString = dateFormat.format(date);
		return "to_date('" + dateString + "','DD-MM-YYYY')";
	}
	
	public static boolean isNumeric(int columnType) {
		try {
			JDBCType type = JDBCType.valueOf(columnType);
			return type == JDBCType.BIGINT || type == JDBCType.DECIMAL || type == JDBCType.DOUBLE
					|| type == JDBCType.FLOAT || type == JDBCType.INTEGER || type == JDBCType.NUMERIC || type == JDBCType.SMALLINT;
		} catch (IllegalArgumentException e) {
			if (JDBCUtils.isOracle()) return columnType == 100 || columnType == 101;
			else return false;
		}
	}
	
	public static boolean isDate(int columnType) {
		try {
			JDBCType type = JDBCType.valueOf(columnType);
			return (type == JDBCType.DATE || type == JDBCType.TIMESTAMP);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	public static String[] select(String sql, String columnName, boolean sort) {
		
		Query query = new Query();
		query.setSql(sql);
		QueryResult result = null;
		
		try {
			result = new QueryExecutor().execute(query);
		} catch (ExportException e) {
			return new String[0];
		}
		
		List<String> values = new ArrayList<String>();
		int colIndex = result.getColumnIndex(columnName);
		
		for (int i=0; i<result.getRowCount(); i++) {
			String value = result.getValueString(i, colIndex);
			if (value != null) values.add(value);
		}

		if (sort) Collections.sort(values);
		return values.toArray(new String[values.size()]);
	}
	
	public static String[][] select(String sql, String[] columnNames) {
		
		Query query = new Query();
		query.setSql(sql);
		QueryResult result = null;
		
		try {
			result = new QueryExecutor().execute(query);
		} catch (ExportException e) {
			return new String[0][];
		}
		
		String[][] values = new String[result.getRowCount()][columnNames.length];
		
		int[] indices = new int[columnNames.length];
		for (int i=0; i<indices.length; i++) {
			indices[i] = result.getColumnIndex(columnNames[i]);
		}
		
		for (int c=0; c<columnNames.length; c++) {
			boolean numeric = result.isColumnNumeric(c);
			for (int i=0; i<result.getRowCount(); i++) {
				values[i][c] = numeric ? ""+result.getNumericValue(i, c) : result.getStringValue(i, c);
			}
		}

		return values;
	}
}
