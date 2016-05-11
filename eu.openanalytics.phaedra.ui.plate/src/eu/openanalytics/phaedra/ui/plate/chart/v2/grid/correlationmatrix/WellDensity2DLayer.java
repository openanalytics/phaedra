package eu.openanalytics.phaedra.ui.plate.chart.v2.grid.correlationmatrix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.density.Density2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellDensity2DLayer extends WellChartLayer {

	private static final int DIMENSION_COUNT = 2;

	@Override
	public String getName() {
		return "Well 2D Density Plot";
	}

	@Override
	public LayerSettings<Plate, Well> createConfig() {
		LayerSettings<Plate, Well> config = new LayerSettings<>();

		updateDataProvider();

		AuxiliaryChartSettings auxSetting = new AuxiliaryChartSettings();
		auxSetting.setWeightFeature(Density2DChart.UNIT_WEIGHT);
		config.getChartSettings().getAuxiliaryChartSettings().add(auxSetting);

		config.setDataProviderSettings(getDataProvider().getDataProviderSettings());

		return config;
	}

	@Override
	public void updateDataProvider() {
		List<String> auxFeatures = new ArrayList<String>();
		auxFeatures.add(Density2DChart.UNIT_WEIGHT);
		getDataProvider().setAuxiliaryFeatures(auxFeatures);
		getDataProvider().setFilters(new ArrayList<IFilter<Plate, Well>>());
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return isDataLoadingJobFinished() ? new WellDensity2DConfigDialog(shell, this) : null;
	}

	@Override
	public WellChartRenderTask createRenderTask(GridCell cell, int w, int h) {
		List<String> selFeatures = new ArrayList<>();
		selFeatures.add(getFeatures().get(cell.getColumn()).getDisplayName());
		selFeatures.add(getFeatures().get(cell.getRow()).getDisplayName());
		return new WellDensity2DRenderTask(getEntities(), selFeatures, getFillOption(), w, h, cell.getRow(), cell.getColumn());
	}

	@Override
	public int getDimensionCount() {
		return DIMENSION_COUNT;
	}
}