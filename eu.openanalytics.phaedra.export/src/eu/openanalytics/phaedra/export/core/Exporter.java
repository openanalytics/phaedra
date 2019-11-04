package eu.openanalytics.phaedra.export.core;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.description.StringValueDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.export.core.ExportSettings.Includes;
import eu.openanalytics.phaedra.export.core.query.Query;
import eu.openanalytics.phaedra.export.core.query.QueryBuilder;
import eu.openanalytics.phaedra.export.core.query.QueryExecutor;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.export.core.statistics.StatisticsFactory;
import eu.openanalytics.phaedra.export.core.writer.IExportWriter;
import eu.openanalytics.phaedra.export.core.writer.WriterFactory;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * This class controls the flow of an well data export process.
 * 
 * It must remain independent of any UI component, to allow non-interactive
 * exports (e.g. batch-triggered or headless).
 */
public class /*WellData*/Exporter {

	/**
	 * Executes the export.
	 * 
	 * @param settings the settings
	 * @param monitor optional progress monitor
	 * @throws ExportException
	 */
	public void export(ExportSettings settings, IProgressMonitor monitor) throws ExportException {
		List<Feature> features = settings.features;

		if (monitor == null) monitor = new NullProgressMonitor();
		monitor.beginTask("Exporting", 1 + 5 + 2*features.size() + 2);
		
		// Validate settings, show errors if needed.
		monitor.subTask("Validating settings");
		new ExportWellsSettingsValidator().validate(settings);
		
		// Initialize the writer.
		IExportWriter writer = WriterFactory.createWriter(settings.fileType);
		try {
			monitor.subTask(String.format("Initializing %1$s writer", settings.getFileType().toUpperCase()));
			writer.initialize(settings);
			monitor.worked(1);
			
			DataFormatter dataFormatter = DataTypePrefs.getDefaultDataFormatter();
			writer.addExportInfo(new ExportInfo(new StringValueDescription("Concentration Unit of Well Compounds", ExportInfo.class),
					dataFormatter.getConcentrationUnit(WellProperty.Concentration.getDataDescription()).getAbbr() ));
			
			// First, perform the base query (columns that are independent of feature).
			monitor.subTask("Querying wells");
			QueryBuilder queryBuilder = new QueryBuilder();
			QueryExecutor queryExecutor = new QueryExecutor();
			queryExecutor.setDataUnitConfig(dataFormatter);
			queryExecutor.setCensoredDataCombine(!settings.getCensoredValueSplit());
			
			Query query = queryBuilder.createWellsQuery(settings);
			QueryResult result = queryExecutor.execute(query);
			writer.writeBaseData(result);
			int expectedRowcount = result.getRowCount();
			monitor.worked(5);
			
			for (Feature feature: features) {
				if (monitor.isCanceled()) {
					writer.rollback();
					return;
				}
				
				// Collect the data
				monitor.subTask("Exporting data for " + feature.getDisplayName());
				query = queryBuilder.createFeatureQuery(feature, settings);
				result = queryExecutor.execute(query, true);
				result.setFeature(feature);
				monitor.worked(1);
				
				if (result.getRowCount() != expectedRowcount) {
					throw new RuntimeException("Column " + feature.getDisplayName() + " has unexpected row count. Expected: "
							+ expectedRowcount + ", actual: " + result.getRowCount());
				}
				
				// Collect additional statistics
				if (settings.includes.contains(Includes.PlateStatistics)) {
					StatisticsFactory.generateStatistics(result, settings);
				}
				
				writer.writeFeature(result);
				monitor.worked(1);
			}
			
			if (monitor.isCanceled()) {
				writer.rollback();
				return;
			}
			
			writer.finish();
			monitor.done();
		} catch (Exception e) {
			writer.rollback();
			throw new ExportException(e.getMessage(), e);
		}
	}
}
