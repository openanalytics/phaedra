package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.correlationmatrix;

import java.util.List;

import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.line.KernelDensity1DWekaChart;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellDensityWeka1DRenderTask extends SubWellScatter2DRenderTask {

	public SubWellDensityWeka1DRenderTask(List<Well> wells, List<String> features, String fillOption,
			DataUnitConfig dataUnitConfig,
			int w, int h, int row, int col) {
		super(wells, features, fillOption, dataUnitConfig, w, h, row, col);
	}

	@Override
	public AbstractChart<Well, Well> getChart() {
		return new KernelDensity1DWekaChart<Well, Well>();
	}

}
