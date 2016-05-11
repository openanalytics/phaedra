package eu.openanalytics.phaedra.ui.plate.chart.v2.view;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.ChartLayerFactory;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.chart.v2.data.WellDataProvider;

public class WellChartLayerFactory extends ChartLayerFactory<Plate, Well> {

	@Override
	public AbstractLegend<Plate, Well> getLegend(ChartName name) {
		switch (name) {
		case SCATTER_2D:
			return new ClassificationScatter2DLegend<Plate, Well>();
		case SCATTER_3D:
			return new ClassificationScatter3DLegend<Plate, Well>();
		default:
			return super.getLegend(name);
		}
	}

	@Override
	public IDataProvider<Plate, Well> getDataProvider() {
		return new WellDataProvider();
	}

}