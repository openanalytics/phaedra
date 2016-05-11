package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.correlationmatrix;

import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DChart;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellScatter2DRenderTask extends SubWellChartRenderTask {

	public SubWellScatter2DRenderTask(List<Well> wells, List<String> features, String fillOption, int w, int h, int row, int col) {
		super(wells, features, fillOption, w, h, row, col);
	}

	@Override
	public AbstractChart<Well, Well> getChart() {
		return new Scatter2DChart<Well, Well>();
	}
}