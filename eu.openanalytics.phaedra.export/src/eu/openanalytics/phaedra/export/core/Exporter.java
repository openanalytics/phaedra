package eu.openanalytics.phaedra.export.core;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.openanalytics.phaedra.export.core.ExportSettings.Includes;
import eu.openanalytics.phaedra.export.core.query.Query;
import eu.openanalytics.phaedra.export.core.query.QueryBuilder;
import eu.openanalytics.phaedra.export.core.query.QueryExecutor;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.export.core.statistics.StatisticsFactory;
import eu.openanalytics.phaedra.export.core.writer.IExportWriter;
import eu.openanalytics.phaedra.export.core.writer.WriterFactory;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * This class controls the flow of an export process.
 * 
 * It must remain independent of any UI component, to allow non-interactive
 * exports (e.g. batch-triggered or headless).
 * 
 * A IProgressMonitor can observe the progress of the export, but this is optional.
 */
public class Exporter {

	public void export(ExportSettings settings, IProgressMonitor monitor) throws ExportException {
		List<Feature> features = settings.features;

		if (monitor == null) monitor = new NullProgressMonitor();
		monitor.beginTask("Exporting", 2*features.size()+6);
		
		// Validate settings, show errors if needed.
		monitor.subTask("Validating settings");
		new SettingsValidator().validate(settings);
		
		// Initialize the writer.
		IExportWriter writer = WriterFactory.createWriter(settings.fileType);
		try {
			monitor.subTask("Initializing " + settings.fileType + " writer");
			writer.initialize(settings);
			monitor.worked(1);
			
			// First, perform the base query (columns that are independent of feature).
			monitor.subTask("Querying wells");
			Query query = new QueryBuilder().createBaseQuery(settings);
			QueryResult result = new QueryExecutor().execute(query);
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
				query = new QueryBuilder().createFeatureQuery(feature, settings);
				result = new QueryExecutor().execute(query, true);
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
			
			writer.finish();
			monitor.done();
		} catch (Exception e) {
			writer.rollback();
			throw new ExportException(e.getMessage(), e);
		}
	}
}
