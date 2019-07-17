package eu.openanalytics.phaedra.calculation.stat.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class PlateStatisticsProvider {

	public static PlateStatistic[] getAvailableStats(Plate plate) {
		if (plate == null)
			return new PlateStatistic[0];

		List<PlateStatistic> stats = new ArrayList<PlateStatistic>();

		stats.add(new PlateStatistic("zprime", "Z-Prime"));
		stats.add(new PlateStatistic("robustzprime", "Robust Z-Prime"));
		stats.add(new PlateStatistic("sn", "Signal/Noise"));
		stats.add(new PlateStatistic("sb", "Signal/Background"));

		List<String> wellTypes = PlateUtils.getWellTypes(plate);
		Collections.sort(wellTypes);
		for (String type : wellTypes) {
			stats.add(new PlateStatistic("count", "Count", type));
			stats.add(new PlateStatistic("mean", "Mean", type));
			stats.add(new PlateStatistic("stdev", "Stdev", type));
			stats.add(new PlateStatistic("cv", "%CV", type));
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
				this.label = welltype + " " + label;
			this.welltype = welltype;
		}

		public double getValue(Plate plate, Feature feature) {
			return StatService.getInstance().calculate(name, plate, feature, welltype,  null);
		}

		public String getFormattedValue(Plate plate, Feature feature) {
			double value = getValue(plate, feature);
			return NumberUtils.round(value, 2);
		}
	}
}
