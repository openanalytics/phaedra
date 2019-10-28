package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.render;

import java.util.Map;

import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.ChartLayerFactory;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.data.SubWellDataProvider;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.view.SubWellChartLayerFactory;

public class SubWellChartRenderTask extends AbstractChartRenderTask<Well, Well> {
	
	
	private final DataUnitConfig dataUnitConfig;
	
	private Map<String, double[][]> plateBoundsMap;

	public SubWellChartRenderTask(Well well,
			DataUnitConfig dataUnitConfig,
			int w, int h, int row, int col) {
		super(w, h, row, col);
		this.dataUnitConfig = dataUnitConfig;
		selection.add(well);
	}

	@Override
	protected boolean isValidChart() {
		return plateBoundsMap != null;
	}

	@Override
	protected void updateBounds(IDataProvider<Well, Well> dataProvider, double[][] dataBounds) {
		SubWellDataProvider swDataProvider = (SubWellDataProvider) dataProvider;
		if (swDataProvider.isUsePlateLimits()) {
			String key = getKey(dataProvider);
			double[][] bounds = plateBoundsMap.get(key);
			for (int i = 0; i < dataBounds.length && i < bounds.length; i++) {
				dataBounds[i][0] = bounds[i][0];
				dataBounds[i][1] = bounds[i][1];
			}
		}
	}

	@Override
	public ChartLayerFactory<Well, Well> getChartLayerFactory() {
		return new SubWellChartLayerFactory(() -> this.dataUnitConfig);
	}

	public void setPlateBoundsMap(Map<String, double[][]> plateBoundsMap) {
		this.plateBoundsMap = plateBoundsMap;
	}

	public static String getKey(IDataProvider<Well, Well> dataProvider) {
		return CollectionUtils.toString(dataProvider.getSelectedFeatures(), ",");
	}

}