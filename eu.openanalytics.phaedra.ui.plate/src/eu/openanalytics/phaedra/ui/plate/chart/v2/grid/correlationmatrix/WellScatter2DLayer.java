package eu.openanalytics.phaedra.ui.plate.chart.v2.grid.correlationmatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.MinMaxFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellScatter2DLayer extends WellChartLayer {

	private static final int DIMENSION_COUNT = 2;

	@Override
	public String getName() {
		return "Well 2D Scatter Plot";
	}

	@Override
	public LayerSettings<Plate, Well> createConfig() {
		LayerSettings<Plate, Well> config = new LayerSettings<>();

		// Restrict nb of filters
		updateDataProvider();

		config.setDataProviderSettings(getDataProvider().getDataProviderSettings());
		return config;
	}

	@Override
	public void updateDataProvider() {
		List<IFilter<Plate, Well>> filters = new ArrayList<IFilter<Plate, Well>>();
		filters.add(new MinMaxFilter<Plate, Well>(getDimensionCount(), getDataProvider()));

		if (getLayerSettings() != null && getLayerSettings().getDataProviderSettings() != null) {
			Map<String, Object> filterProperties = getLayerSettings().getDataProviderSettings().getFilterProperties();
			for (IFilter<Plate, Well> filter : filters) {
				if (filterProperties.containsKey(filter.getClass().getName())) {
					filter.setProperties(filterProperties.get(filter.getClass().getName()));
				}
			}
		}

		getDataProvider().setFilters(filters);
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return isDataLoadingJobFinished() ? new WellScatter2DConfigDialog(shell, this) : null;
	}

	@Override
	public WellScatter2DRenderTask createRenderTask(GridCell cell, int w, int h) {
		List<String> selFeatures = new ArrayList<>();
		selFeatures.add(getFeatures().get(cell.getColumn()).getDisplayName());
		selFeatures.add(getFeatures().get(cell.getRow()).getDisplayName());
		return new WellScatter2DRenderTask(getEntities(), selFeatures, getFillOption(),
				getDataFormatter(),
				w, h, cell.getRow(), cell.getColumn() );
	}

	@Override
	public int getDimensionCount() {
		return DIMENSION_COUNT;
	}
}
