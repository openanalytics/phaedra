package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;

public class SubWellParallelCoordChartLayer extends AbstractSubWellChartLayer {

	private static final String NAME = "Subwell Parallel Coordinate Chart Layer";
	private static final String VIEW_ID = "eu.openanalytics.phaedra.ui.subwell.chart.v2.view.SubWellParallelCoordinatesView";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getDimensionCount() {
		return ChartType.DYNAMIC.getNumberOfDimensions();
	}

	@Override
	public void updateDataProvider() {
		// Do nothing.
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return new ChartConfigDialog(shell, this, NAME, VIEW_ID);
	}

}
