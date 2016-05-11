package eu.openanalytics.phaedra.ui.subwell.chart.v2.view;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.ChartLayerFactory;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.chart.v2.view.ClassificationScatter2DLegend;
import eu.openanalytics.phaedra.ui.plate.chart.v2.view.ClassificationScatter3DLegend;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.data.SubWellDataProvider;

public class SubWellChartLayerFactory extends ChartLayerFactory<Well, Well> {

	@Override
	public AbstractLegend<Well, Well> getLegend(ChartName name) {
		switch (name) {
		case SCATTER_2D:
			return new ClassificationScatter2DLegend<Well, Well>();
		case SCATTER_3D:
			return new ClassificationScatter3DLegend<Well, Well>();
		default:
			return super.getLegend(name);
		}
	}

	@Override
	public IDataProvider<Well, Well> getDataProvider() {
		return new SubWellDataProvider();
	}

}