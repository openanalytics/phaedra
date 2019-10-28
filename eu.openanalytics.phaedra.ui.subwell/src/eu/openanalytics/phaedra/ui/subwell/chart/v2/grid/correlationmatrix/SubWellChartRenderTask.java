package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.correlationmatrix;

import java.util.List;

import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.ui.charting.v2.EntityRenderTask;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.data.SubWellDataProvider;
import eu.openanalytics.phaedra.ui.subwell.grid.correlationmatrix.FeatureWellLayer;

public abstract class SubWellChartRenderTask extends EntityRenderTask<Well, Well> {

	private List<String> features;

	private String fillOption;
	private final DataUnitConfig dataUnitConfig;
	
	
	public SubWellChartRenderTask(List<Well> wells, List<String> features, String fillOption,
			DataUnitConfig dataUnitConfig,
			int w, int h, int row, int col) {
		super(w, h, row, col);
		this.selection.addAll(wells);
		this.features = features;
		this.fillOption = fillOption;
		this.dataUnitConfig = dataUnitConfig;
	}

	@Override
	protected boolean isValidChart() {
		if (fillOption.equals(FeatureWellLayer.BOTTOM_LEFT) && getRow() <= getCol()) {
			return false;
		}
		if (fillOption.equals(FeatureWellLayer.TOP_RIGHT) && getRow() >= getCol()) {
			return false;
		}
		if (fillOption.equals(FeatureWellLayer.DIAGONAL) && getRow() != getCol()) {
			return false;
		}
		if (fillOption.equals(FeatureWellLayer.EXCLUDE_GIAGONAL) && getRow() == getCol()) {
			return false;
		}
		return super.isValidChart();
	}

	@Override
	protected void updateChartDataProvider(IDataProvider<Well, Well> dataProvider) {
		dataProvider.setSelectedFeatures(features);
	}

	@Override
	public abstract AbstractChart<Well, Well> getChart();

	@Override
	public IDataProvider<Well, Well> getChartDataProvider() {
		return new SubWellDataProvider(() -> this.dataUnitConfig);
	}
	
}
