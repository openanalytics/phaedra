package eu.openanalytics.phaedra.export.core;

import static eu.openanalytics.phaedra.base.util.CollectionUtils.containsAny;
import static eu.openanalytics.phaedra.export.core.ExportPlateTableSettings.Includes.FeatureControlStatistics;
import static eu.openanalytics.phaedra.export.core.ExportPlateTableSettings.Includes.FeatureStatistics;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import eu.openanalytics.phaedra.base.datatype.description.RealValueDescription;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.export.core.query.Query;
import eu.openanalytics.phaedra.export.core.query.QueryBuilder;
import eu.openanalytics.phaedra.export.core.query.QueryExecutor;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.export.core.writer.IExportWriter;
import eu.openanalytics.phaedra.export.core.writer.WriterFactory;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;

/**
 * This class controls the flow of an plate list export process.
 * 
 * It must remain independent of any UI component, to allow non-interactive
 * exports (e.g. batch-triggered or headless).
 */
public class PlateTableExporter {
	
	
	public PlateTableExporter() {
	}
	
	
	/**
	 * Executes the export.
	 * 
	 * @param settings the settings
	 * @param monitor optional progress monitor
	 * @throws ExportException
	 */
	public void export(ExportPlateTableSettings settings, IProgressMonitor monitor) throws ExportException {
		boolean withFeatureStats = containsAny(settings.getIncludes(), FeatureStatistics, FeatureControlStatistics);
		SubMonitor m = SubMonitor.convert(monitor, "Exporting", 1 + 5 + ((withFeatureStats) ? 4 : 0));
		
		// Validate settings, show errors if needed.
		m.subTask("Validating settings");
		new ExportPlateTableSettingsValidator().validate(settings);
		
		// Initialize the writer.
		IExportWriter writer = WriterFactory.createWriter(settings.getFileType());
		try {
			monitor.subTask(String.format("Initializing %1$s writer", settings.getFileType().toUpperCase()));
			writer.initialize(settings);
			m.worked(1);
			
			// First, perform the base query (columns that are independent of feature).
			monitor.subTask("Querying plates");
			QueryBuilder queryBuilder = new QueryBuilder();
			QueryExecutor queryExecutor = new QueryExecutor();
			Query query = queryBuilder.createPlatesQuery(settings);
			QueryResult baseResult = queryExecutor.execute(query);
			writer.writeBaseData(baseResult);
			
			m.worked(5);
			
			if (withFeatureStats) {
				m.subTask("Generate statistics");
				writeFeatureStats(baseResult, settings, writer, m.newChild(4));
			}
			
			if (m.isCanceled()) {
				writer.rollback();
				return;
			}
			
			writer.finish();
			m.done();
		} catch (Exception e) {
			writer.rollback();
			throw new ExportException("Failed to export data", e);
		}
	}
	
	private void writeFeatureStats(QueryResult baseResult, ExportPlateTableSettings settings,
			IExportWriter writer, SubMonitor m) throws IOException {
		QueryResult featureStats = new QueryResult();
		boolean includeFeatureStats = settings.getIncludes().contains(FeatureStatistics);
		boolean includeFeatureControlStats = settings.getIncludes().contains(FeatureControlStatistics);
		List<Feature> features = settings.getFeatures();
		int statCount = 0;
		if (includeFeatureStats) statCount += 8;
		if (includeFeatureControlStats) statCount += 4;
		int colCount = features.size() * statCount;
		int rowCount = baseResult.getRowCount();
		
		m.setWorkRemaining(rowCount);
		
		{	// Header
			for (Feature feature : features) {
				if (includeFeatureStats) {
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + "Z-Prime", Well.class));
					
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + "Robust Z-Prime", Well.class));
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + "Pearson Corr Coeff", Well.class));
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + "Pearson p-value", Well.class));
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + "Spearman Corr Coeff", Well.class));
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + "Spearman p-value", Well.class));
					
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + "S/N", Well.class));
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + "S/B", Well.class));
				}
				if (includeFeatureControlStats) {
					String lowTypeCode = ProtocolUtils.getLowType(feature);
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + lowTypeCode + " Mean", Well.class));
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + lowTypeCode + " %CV", Well.class));
					String highTypeCode = ProtocolUtils.getHighType(feature);
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + highTypeCode + " Mean", Well.class));
					featureStats.addColumn(new RealValueDescription(feature.getName() + " " + highTypeCode + " %CV", Well.class));
				}
			}
		}
		
		PlateService plateService = PlateService.getInstance();
		StatService statService = StatService.getInstance();
		int colPlateId = baseResult.getColumnIndex("PLATE_ID");
		for (int row = 0; row < rowCount; row++) {
			if (m.isCanceled()) {
				return;
			}
			
			Plate plate = plateService.getPlateById(baseResult.getLongValue(row, colPlateId));
			Object[] rowValues = new Object[colCount];
			int col = 0;
			for (Feature feature : features) {
				if (includeFeatureStats) {
					rowValues[col++] = statService.calculate("zprime", plate, feature, null, null);
					
					rowValues[col++] = statService.calculate("robustzprime", plate, feature, null, null);
					rowValues[col++] = statService.calculate("pearsoncc", plate, feature, null, null);
					rowValues[col++] = statService.calculate("pearsonpval", plate, feature, null, null);
					rowValues[col++] = statService.calculate("spearmancc", plate, feature, null, null);
					rowValues[col++] = statService.calculate("spearmanpval", plate, feature, null, null);
					
					rowValues[col++] = statService.calculate("sn", plate, feature, null, null);
					rowValues[col++] = statService.calculate("sb", plate, feature, null, null);
				}
				if (includeFeatureControlStats) {
					String lowTypeCode = ProtocolUtils.getLowType(feature);
					WellType lowWellType = ProtocolService.getInstance().getWellTypeByCode(lowTypeCode).orElse(null);
					rowValues[col++] = statService.calculate("mean", plate, feature, lowWellType, null);
					rowValues[col++] = statService.calculate("cv", plate, feature, lowWellType, null);
					String highTypeCode = ProtocolUtils.getHighType(feature);
					WellType highWellType = ProtocolService.getInstance().getWellTypeByCode(highTypeCode).orElse(null);
					rowValues[col++] = statService.calculate("mean", plate, feature, highWellType, null);
					rowValues[col++] = statService.calculate("cv", plate, feature, highWellType, null);
				}
			}
			featureStats.addRow(rowValues);
			m.worked(1);
		}
		featureStats.finish();
		writer.writeFeature(featureStats);
	}
	
}
