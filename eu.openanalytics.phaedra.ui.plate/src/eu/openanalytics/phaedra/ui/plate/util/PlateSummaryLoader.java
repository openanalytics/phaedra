package eu.openanalytics.phaedra.ui.plate.util;

import java.util.List;
import java.util.function.Consumer;

import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateSummary;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.AbstractSummaryLoader;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class PlateSummaryLoader extends AbstractSummaryLoader<Plate, PlateSummary> {

	private static final PlateSummary EMPTY_SUMMARY = new PlateSummary();

	public PlateSummaryLoader(List<Plate> input, Consumer<Plate> refreshCallback) {
		super(input, refreshCallback);
	}

	@Override
	protected PlateSummary getEmptySummary() {
		return EMPTY_SUMMARY;
	}

	@Override
	protected PlateSummary createSummary(Plate item) {
		PlateSummary summary = PlateService.getInstance().getPlateSummary(item);
		StatService.getInstance().loadPersistentPlateStats(item);
		return new PlateSummaryWithStats(item, summary);
	}

	public static class PlateSummaryWithStats extends PlateSummary {
		
		private Plate plate;
		
		public PlateSummaryWithStats(Plate plate, PlateSummary summary) {
			this.plate = plate;
			this.crcCount = summary.crcCount;
			this.screenCount = summary.screenCount;
		}
		
		@Override
		public double getStat(String stat, Feature f, String wellType, String norm) {
			return StatService.getInstance().calculate(stat, plate, f, wellType, norm);
		}
	}
}
