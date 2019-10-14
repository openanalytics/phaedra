package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.correlationmatrix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellDensityWeka1DLayer extends SubWellScatter2DLayer {

	private static final int DIMENSION_COUNT = 1;

	@Override
	public String getName() {
		return "SubWell 1D Density Plot";
	}

	@Override
	public SubWellDensityWeka1DRenderTask createRenderTask(GridCell cell, int w, int h) {
		List<String> selFeatures = new ArrayList<>();
		selFeatures.add(getFeatures().get(cell.getColumn()).getDisplayName());
		return new SubWellDensityWeka1DRenderTask(getEntities(), selFeatures, getFillOption(),
				getDataFormatter(),
				w, h, cell.getRow(), cell.getColumn() );
	}

	@Override
	public LayerSettings<Well, Well> createConfig() {
		LayerSettings<Well, Well> config = new LayerSettings<>();
		config.getChartSettings().setLines(true);

		updateDataProvider();

		getDataProvider().setFilters(new ArrayList<IFilter<Well, Well>>());

		config.setDataProviderSettings(getDataProvider().getDataProviderSettings());

		return config;
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return isDataLoadingJobFinished() ? new SubWellDensityWeka1DConfigDialog(shell, this) : null;
	}

	@Override
	public int getDimensionCount() {
		return DIMENSION_COUNT;
	}

	@Override
	public String getDefaultFillOption() {
		return DIAGONAL;
	}

}