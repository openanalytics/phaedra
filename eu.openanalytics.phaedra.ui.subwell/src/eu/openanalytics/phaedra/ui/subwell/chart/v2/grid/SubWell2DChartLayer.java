package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;

public class SubWell2DChartLayer extends AbstractSubWellChartLayer {

	private static final String NAME = "Subwell 2D Chart Layer";
	private static final String VIEW_ID = "eu.openanalytics.phaedra.ui.subwell.chart.v2.view.SubWellScatter2DView";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getDimensionCount() {
		return ChartType.TWO_DIMENSIONAL.getNumberOfDimensions();
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
