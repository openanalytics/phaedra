package eu.openanalytics.phaedra.ui.silo.chart.layer;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.ChartLayerFactory;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.chart.data.SiloDataProvider;

public class SiloChartLayerFactory extends ChartLayerFactory<Silo, Silo> {

	@Override
	public IDataProvider<Silo, Silo> getDataProvider() {
		return new SiloDataProvider();
	}

}