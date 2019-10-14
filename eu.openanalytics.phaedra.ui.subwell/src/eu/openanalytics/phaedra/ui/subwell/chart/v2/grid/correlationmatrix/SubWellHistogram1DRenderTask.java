package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.correlationmatrix;

import java.util.List;

import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.histogram.Histogram1DChart;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellHistogram1DRenderTask extends SubWellChartRenderTask{

	public SubWellHistogram1DRenderTask(List<Well> wells, List<String> features, String fillOption,
			DataUnitConfig dataUnitConfig,
			int w, int h, int row, int col) {
		super(wells, features, fillOption, dataUnitConfig, w, h, row, col);
	}

	@Override
	public AbstractChart<Well, Well> getChart() {
		return new Histogram1DChart<Well, Well>();
	}
}
