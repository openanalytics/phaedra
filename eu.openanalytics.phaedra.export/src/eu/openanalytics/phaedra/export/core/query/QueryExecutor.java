package eu.openanalytics.phaedra.export.core.query;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.export.Activator;
import eu.openanalytics.phaedra.export.core.ExportException;
import eu.openanalytics.phaedra.export.core.util.SQLUtils;

/**
 * This class obtains a JDBC connection and executes
 * an SQL query. If the query fails, an ExportException will be thrown.
 */
public class QueryExecutor {

	public QueryResult execute(Query query) throws ExportException {
		return execute(query, false);
	}

	public QueryResult execute(Query query, boolean isFeatureQuery) throws ExportException {

		QueryResult result = new QueryResult(query);
		String sql = query.getSql();

		ResultSet resultSet = null;
		Statement stmt = null;

		long startTime = System.currentTimeMillis();
		
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			stmt = conn.createStatement();
			resultSet = stmt.executeQuery(sql);
			ResultSetMetaData metadata = resultSet.getMetaData();
			
			// Either get all column names, or in the case of feature queries, a subset.
			String[] headers = null;
			if (isFeatureQuery) {
				headers = new String[metadata.getColumnCount() - 2];
				for (int i=0; i<headers.length; i++) {
					headers[i] = metadata.getColumnName(i+3);
					if (SQLUtils.isNumeric(metadata.getColumnType(i+3))) result.setNumericColumn(i);
				}
				result.setColumns(headers);
			} else {
				headers = new String[metadata.getColumnCount()];
				for (int i=0; i<headers.length; i++) {
					headers[i] = metadata.getColumnName(i+1);
					if (SQLUtils.isNumeric(metadata.getColumnType(i+1))) result.setNumericColumn(i);
				}
				result.setColumns(headers);
			}
			int columnCount = headers.length;
			
			String featureName = null;

			while (resultSet.next()) {
				// Sample the feature name of this result set.
				if (isFeatureQuery && featureName == null) {
					featureName = resultSet.getString(1);
				}

				Object[] values = new Object[columnCount];
				for (int i=0; i<columnCount; i++) {
					String header = headers[i];
					values[i] = resultSet.getObject(header);
				}
				result.addRow(values);
			}
			result.finish();

			if (isFeatureQuery && featureName != null) {
				for (int i=0; i<headers.length; i++) {
					headers[i] = featureName + " " + headers[i];
				}
				result.setColumns(headers);
			}

			long duration = System.currentTimeMillis() - startTime;
			EclipseLog.info("Query executed in " + duration + "ms.", Activator.getDefault());
			
		} catch (SQLException e) {
			throw new ExportException("Query execution failed: " + e.getMessage(), e);
		} finally {
			if (resultSet != null) try { resultSet.close(); } catch (SQLException e) {}
			if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
		}

		return result;
	}
}
