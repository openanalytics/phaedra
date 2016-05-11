package eu.openanalytics.phaedra.ui.plate.util;

import java.util.List;
import java.util.function.Consumer;

import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.ExperimentSummary;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.util.AbstractSummaryLoader;

public class ExperimentSummaryLoader extends AbstractSummaryLoader<Experiment, ExperimentSummary> {

	private static final ExperimentSummary EMPTY_SUMMARY = new ExperimentSummary();

	public ExperimentSummaryLoader(List<Experiment> input, Consumer<Experiment> refreshCallback) {
		super(input, refreshCallback);
	}

	@Override
	protected ExperimentSummary getEmptySummary() {
		return EMPTY_SUMMARY;
	}

	@Override
	protected ExperimentSummary createSummary(Experiment item) {
		return PlateService.getInstance().getExperimentSummary(item);
	}

}
