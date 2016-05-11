package eu.openanalytics.phaedra.ui.plate.chart.v2.grid.correlationmatrix;

import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DChart;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellScatter2DRenderTask extends WellChartRenderTask {

	public WellScatter2DRenderTask(List<Well> wells, List<String> features, String fillOption, int w, int h, int row, int col) {
		super(wells, features, fillOption, w, h, row, col);
	}

	@Override
	public AbstractChart<Plate, Well> getChart() {
		return new Scatter2DChart<Plate, Well>();
	}
}