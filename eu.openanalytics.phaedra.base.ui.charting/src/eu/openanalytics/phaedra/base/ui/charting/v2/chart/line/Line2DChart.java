package eu.openanalytics.phaedra.base.ui.charting.v2.chart.line;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.LINE_2D;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DChart;
import uk.ac.starlink.ttools.plot.ScatterPlot;

public class Line2DChart<ENTITY, ITEM> extends Scatter2DChart<ENTITY, ITEM> {

	public Line2DChart() {
		super();
		setName(LINE_2D);
	}

	@Override
	public ScatterPlot createPlot() {
		getChartSettings().setLines(true);
		return super.createPlot();
	}

	@Override
	public boolean isSupportSVG() {
		return false;
	}

}