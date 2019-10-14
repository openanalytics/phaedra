package eu.openanalytics.phaedra.ui.plate.chart.v2.grid.correlationmatrix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellHistogram1DLayer extends WellChartLayer {

	private static final int DIMENSION_COUNT = 1;

	@Override
	public String getName() {
		return "Well 1D Histogram Plot";
	}

	@Override
	public WellChartRenderTask createRenderTask(GridCell cell, int w, int h) {
		List<String> selFeatures = new ArrayList<>();
		selFeatures.add(getFeatures().get(cell.getColumn()).getDisplayName());
		return new WellHistogram1DRenderTask(getEntities(), selFeatures, getFillOption(),
				getDataFormatter(),
				w, h, cell.getRow(), cell.getColumn() );
	}

	@Override
	public LayerSettings<Plate, Well> createConfig() {
		LayerSettings<Plate, Well> config = new LayerSettings<>();
		config.getChartSettings().setBars(true);

		updateDataProvider();

		config.setDataProviderSettings(getDataProvider().getDataProviderSettings());

		return config;
	}

	@Override
	public void updateDataProvider() {
		getDataProvider().setFilters(new ArrayList<IFilter<Plate, Well>>());
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return isDataLoadingJobFinished() ? new WellHistogram1DConfigDialog(shell, this) : null;
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
