package eu.openanalytics.phaedra.calculation.stat.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;

public class PlateStatisticsProvider {

	public static PlateStatistic[] getAvailableStats(Plate plate) {
		if (plate == null)
			return new PlateStatistic[0];

		List<PlateStatistic> stats = new ArrayList<PlateStatistic>();

		stats.add(new PlateStatistic("zprime", "Z-Prime"));
		stats.add(new PlateStatistic("robustzprime", "Robust Z-Prime"));
		
		// UR-015: Add Pearson and Spearman correlation coefficient 
		stats.add(new PlateStatistic("pearsoncc", "Pearson correlation coefficient"));
		stats.add(new PlateStatistic("pearsonpval", "Pearson p-value"));
		stats.add(new PlateStatistic("spearmancc", "Spearman correlation coefficient"));
		stats.add(new PlateStatistic("spearmanpval", "Spearman p-value"));
		
		stats.add(new PlateStatistic("sn", "Signal/Noise"));
		stats.add(new PlateStatistic("sb", "Signal/Background"));

		List<String> wellTypes = PlateUtils.getWellTypes(plate);
		Collections.sort(wellTypes);
		for (String wellType : wellTypes) {
			stats.add(new PlateStatistic("count", "Count", wellType));
			stats.add(new PlateStatistic("mean", "Mean", wellType));
			stats.add(new PlateStatistic("stdev", "Stdev", wellType));
			stats.add(new PlateStatistic("cv", "%CV", wellType));
		}

		return stats.toArray(new PlateStatistic[stats.size()]);
	}

	public static class PlateStatistic {

		public String name;
		public String label;
		public String welltype = null;

		public PlateStatistic() {
			// Default constructor
		}

		public PlateStatistic(String name, String label) {
			this.name = name;
			this.label = label;
		}

		public PlateStatistic(String name, String label, String welltype) {
			this.name = name;
			if (welltype == null)
				this.label = label;
			else
				this.label = ProtocolUtils.getCustomHCLCLabel(welltype) + " " + label; // PHA-644
			this.welltype = welltype;
		}

		public double getValue(Plate plate, Feature feature) {
			// PHA-644
			WellType wellTypeObject = ProtocolService.getInstance().getWellTypeByCode(welltype).orElse(null);
			return StatService.getInstance().calculate(name, plate, feature, wellTypeObject,  null);
		}

		public String getFormattedValue(Plate plate, Feature feature) {
			double value = getValue(plate, feature);
			return NumberUtils.round(value, 2);
		}
	}
}
