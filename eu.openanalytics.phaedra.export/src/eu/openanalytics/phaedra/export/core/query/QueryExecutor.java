package eu.openanalytics.phaedra.export.core.query;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.conversion.IConverter;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.description.CensoredValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.ConcentrationDataDescription;
import eu.openanalytics.phaedra.base.datatype.description.ContentType;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.description.RealValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.StringValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.TimestampDescription;
import eu.openanalytics.phaedra.base.datatype.format.ConcentrationFormat;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.export.core.ExportException;
import eu.openanalytics.phaedra.export.core.query.QueryResult.Column;
import eu.openanalytics.phaedra.export.core.util.SQLUtils;

/**
 * This class obtains a JDBC connection and executes
 * an SQL query. If the query fails, an ExportException will be thrown.
 */
public class QueryExecutor {
	
	
	private static abstract class DataSupplier {
		
		protected final ResultSet resultSet;
		
		public DataSupplier(final ResultSet resultSet) {
			this.resultSet = resultSet;
		}
		
		public abstract Object getObjectValue() throws SQLException;
		
	}
	
	private static final class DefaultDataSupplier extends DataSupplier {
		
		private final int column;
		
		public DefaultDataSupplier(final ResultSet resultSet, final int column) {
			super(resultSet);
			this.column = column;
		}
		
		@Override
		public Object getObjectValue() throws SQLException {
			return this.resultSet.getObject(this.column);
		}
		
	}
	
	private static final class ConvertDataSupplier extends DataSupplier {
		
		private final int column;
		private final IConverter converter;
		
		public ConvertDataSupplier(final ResultSet resultSet, final int column,
				final IConverter converter) {
			super(resultSet);
			this.column = column;
			this.converter = converter;
		}
		
		@Override
		public Object getObjectValue() throws SQLException {
			return this.converter.convert(this.resultSet.getObject(this.column));
		}
		
	}
	
	private static final class CensoredConcentrationDataSupplier extends DataSupplier {
		
		private final int valueColumn;
		private final int censorColumn;
		private final ConcentrationFormat format;
		private final ConcentrationUnit sourceUnit;
		
		public CensoredConcentrationDataSupplier(final ResultSet resultSet, final int valueColumn, final int censorColumn,
				final ConcentrationFormat format, final ConcentrationUnit sourceUnit) {
			super(resultSet);
			this.valueColumn = valueColumn;
			this.censorColumn = censorColumn;
			this.format = format;
			this.sourceUnit = sourceUnit;
		}
		
		@Override
		public Object getObjectValue() throws SQLException {
			final double conc = resultSet.getDouble(valueColumn);
			if (resultSet.wasNull()) return null;
			return format.format(resultSet.getString(censorColumn), conc, this.sourceUnit);
		}
		
	}
	
	
	private DataFormatter dataFormatter;
	
	private boolean isCensoredDataCombine;
	
	
	public QueryExecutor() {
	}
	
	
	public void setDataUnitConfig(final DataFormatter config) {
		this.dataFormatter = config;
	}
	
	public void setCensoredDataCombine(final boolean enable) {
		this.isCensoredDataCombine = enable;
	}
	

	public QueryResult execute(Query query) throws ExportException {
		return execute(query, false);
	}

	public QueryResult execute(Query query, boolean isFeatureQuery) throws ExportException {
		QueryResult result = new QueryResult(query);
		String sql = query.getSql();
		if (this.dataFormatter == null) this.dataFormatter = DataTypePrefs.getDefaultDataFormatter();

		ResultSet resultSet = null;
		Statement stmt = null;

		long startTime = System.currentTimeMillis();

		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			stmt = conn.createStatement();
			resultSet = stmt.executeQuery(sql);
			ResultSetMetaData metadata = resultSet.getMetaData();
			
			// Either get all column names, or in the case of feature queries, a subset.
			int columnCount;
			int sqlOffset;
			if (isFeatureQuery) {
				columnCount = metadata.getColumnCount() - 2;
				sqlOffset = 3;
			}
			else {
				columnCount = metadata.getColumnCount();
				sqlOffset = 1;
			}
			List<DataSupplier> dataSuppliers = new ArrayList<DataSupplier>(columnCount);
			boolean[] skip = new boolean[sqlOffset + columnCount];
			for (int i = 0; i < columnCount; i++) {
				final int resultColumn = i + sqlOffset;
				if (skip[resultColumn]) continue;
				
				String columnLabel = metadata.getColumnLabel(resultColumn).toUpperCase();
				DataDescription dataDescription = query.getColumnDataType(columnLabel);
				if (dataDescription != null) {
					DataDescription targetDescr = dataDescription.alterTo(this.dataFormatter);
					String targetLabel = (dataDescription.getName() == columnLabel) ?
							targetDescr.getName() :
							dataDescription.convertNameTo(columnLabel, this.dataFormatter);
					if (targetLabel != columnLabel) targetLabel = targetLabel.toUpperCase();
					if (this.isCensoredDataCombine
							&& dataDescription.getDataType() == DataType.Real
							&& dataDescription.getContentType() == ContentType.Concentration
							&& dataDescription instanceof CensoredValueDescription) {
						int censorColumn = resultSet.findColumn(Query.checkColumnLabel(
								((CensoredValueDescription)dataDescription).getCensorName()));
						result.addColumn(new StringValueDescription(targetLabel));
						dataSuppliers.add(new CensoredConcentrationDataSupplier(resultSet,
								resultColumn, censorColumn,
								this.dataFormatter.getConcentrationFormat(),
								((ConcentrationDataDescription)dataDescription).getConcentrationUnit()));
						skip[censorColumn] = true;
					}
					else {
						result.addColumn(dataDescription, targetLabel);
						final IConverter converter = dataDescription.getDataConverterTo(this.dataFormatter);
						dataSuppliers.add((converter != null) ?
								new ConvertDataSupplier(resultSet, resultColumn, converter) :
								new DefaultDataSupplier(resultSet, resultColumn));
					}
				}
				else {
					result.addColumn(getColumnDescription(columnLabel, metadata, resultColumn));
					dataSuppliers.add(new DefaultDataSupplier(resultSet, resultColumn));
				}
			}
			
			String featureName = null;

			final int n = dataSuppliers.size();
			while (resultSet.next()) {
				// Sample the feature name of this result set.
				if (isFeatureQuery && featureName == null) {
					featureName = resultSet.getString(1);
				}

				Object[] values = new Object[n];
				for (int i = 0; i < n; i++) {
					values[i] = dataSuppliers.get(i).getObjectValue();
				}
				result.addRow(values);
			}

			// Prepend feature name to name, if needed.
			if (isFeatureQuery && featureName != null) {
				for (final Column column : result.getColumns()) {
					String name = column.getName();
					name = featureName + " " + name;
					column.setName(name);
				}
			}
			result.finish();

			long duration = System.currentTimeMillis() - startTime;
			EclipseLog.debug("Query executed in " + duration + "ms.", QueryExecutor.class);
			
		} catch (SQLException e) {
			throw new ExportException("Query execution failed: " + e.getMessage(), e);
		} finally {
			if (resultSet != null) try { resultSet.close(); } catch (SQLException e) {}
			if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
		}

		return result;
	}
	
	private DataDescription getColumnDescription(String name, ResultSetMetaData metadata, int column) throws SQLException {
		int sqlType = metadata.getColumnType(column);
		if (SQLUtils.isNumeric(sqlType)) return new RealValueDescription(name);
		if (SQLUtils.isDate(sqlType)) return new TimestampDescription(name);
		return new StringValueDescription(name);
	}
}
