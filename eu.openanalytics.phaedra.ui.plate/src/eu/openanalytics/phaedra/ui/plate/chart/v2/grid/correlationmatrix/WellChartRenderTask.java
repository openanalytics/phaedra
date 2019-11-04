package eu.openanalytics.phaedra.ui.plate.chart.v2.grid.correlationmatrix;

import java.util.List;

import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.ui.charting.v2.EntityRenderTask;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.chart.v2.data.WellDataProvider;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.FeaturePlateLayer;

public abstract class WellChartRenderTask extends EntityRenderTask<Plate, Well> {

	private List<String> features;

	private String fillOption;
	private final DataUnitConfig dataUnitConfig;
	
	
	public WellChartRenderTask(List<Well> wells, List<String> features, String fillOption,
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
		if (fillOption.equals(FeaturePlateLayer.BOTTOM_LEFT) && getRow() <= getCol()) {
			return false;
		}
		if (fillOption.equals(FeaturePlateLayer.TOP_RIGHT) && getRow() >= getCol()) {
			return false;
		}
		if (fillOption.equals(FeaturePlateLayer.DIAGONAL) && getRow() != getCol()) {
			return false;
		}
		if (fillOption.equals(FeaturePlateLayer.EXCLUDE_GIAGONAL) && getRow() == getCol()) {
			return false;
		}
		return super.isValidChart();
	}

	@Override
	protected void updateChartDataProvider(IDataProvider<Plate, Well> dataProvider) {
		dataProvider.setSelectedFeatures(features);
	}

	@Override
	public abstract AbstractChart<Plate, Well> getChart();

	@Override
	public IDataProvider<Plate, Well> getChartDataProvider() {
		return new WellDataProvider(() -> this.dataUnitConfig);
	}
	
}
