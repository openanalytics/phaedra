package eu.openanalytics.phaedra.export.core.statistics;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class StatisticsFactory {

	public static void generateStatistics(QueryResult result, IExportExperimentsSettings settings) {

		// Collect all plates, query if necessary.
		List<Plate> allPlates = new ArrayList<Plate>();
		for (Experiment exp : settings.getExperiments()) {
			allPlates.addAll(PlateService.getInstance().getPlates(exp));
		}

		Feature feature = result.getFeature();
		Statistics exportStats = new Statistics();

		// First pass, collect names of all control types
		List<String> controlTypes = new ArrayList<String>();
		for (Plate plate : allPlates) {
			for (String welltype : PlateUtils.getWellTypes(plate)) {
				CollectionUtils.addUnique(controlTypes, welltype);
			}
		}

		int statCount = 3 + (4 * controlTypes.size());
		double[][] values = new double[statCount][allPlates.size()];

		// Second pass, collect statistics for all control types
		for (int i=0; i<allPlates.size(); i++) {
			Plate plate = allPlates.get(i);
			
			double zprime = StatService.getInstance().calculate("zprime", plate, feature, null, null);
			double sb = StatService.getInstance().calculate("sb", plate, feature, null, null);
			double sn = StatService.getInstance().calculate("sn", plate, feature, null, null);
			
			values[0][i] = zprime;
			values[1][i] = sb;
			values[2][i] = sn;
			
			int index = 3;
			for (String controlType : controlTypes) {
				double count = StatService.getInstance().calculate("count", plate, feature, controlType, null);
				double mean = StatService.getInstance().calculate("mean", plate, feature, controlType, null);
				double stdev = StatService.getInstance().calculate("stdev", plate, feature, controlType, null);
				double cv = StatService.getInstance().calculate("cv", plate, feature, controlType, null);
				
				values[index++][i] = count;
				values[index++][i] = mean;
				values[index++][i] = stdev;
				values[index++][i] = cv;
			}
		}

		exportStats.set("zPrime", StatService.getInstance().calculate("mean", values[0]));
		exportStats.set("Sig/Noise", StatService.getInstance().calculate("mean", values[1]));
		exportStats.set("Sig/Bg", StatService.getInstance().calculate("mean", values[2]));
		int index = 3;
		for (String controlType : controlTypes) {
			exportStats.set(controlType + " Count", StatService.getInstance().calculate("sum", values[index++]));
			exportStats.set(controlType + " Mean", StatService.getInstance().calculate("mean", values[index++]));
			exportStats.set(controlType + " Stdev", StatService.getInstance().calculate("mean", values[index++]));
			exportStats.set(controlType + " %CV", StatService.getInstance().calculate("mean", values[index++]));
		}

		result.setStatistics(exportStats);
	}
}
