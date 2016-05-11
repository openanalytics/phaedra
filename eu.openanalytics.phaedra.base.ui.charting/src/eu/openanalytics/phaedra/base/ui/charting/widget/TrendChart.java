package eu.openanalytics.phaedra.base.ui.charting.widget;

import java.awt.Color;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;

import eu.openanalytics.phaedra.base.ui.charting.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.render.IRenderCustomizer;

public class TrendChart<E> extends ChartComposite {

	private IDataProvider<E> dataProvider;
	private IRenderCustomizer renderCustomizer;
	
	public TrendChart(Composite comp, int style) {
		super(
				comp, style, null,
				400, 300, 50, 50, 1500, 1500,
				true, false, false, false, false, false);
	}

	public void setDataProvider(IDataProvider<E> dataProvider) {
		this.dataProvider = dataProvider;
		buildChart();
	}
	
	public void setRenderCustomizer(IRenderCustomizer renderCustomizer) {
		this.renderCustomizer = renderCustomizer;
	}
	
	protected void buildChart() {
		
		if (dataProvider == null) return;

		// Build a dataset.
		XYDataset dataset = createDataset(dataProvider);
		
		// Build a plot.
		XYPlot plot = new XYPlot();
		plot.setDataset(dataset);
		plot.setOrientation(PlotOrientation.VERTICAL);
		
		plot.setDomainAxis(dataProvider.createAxis(0, null));
		plot.setRangeAxis(dataProvider.createAxis(1, null));
		
		JFreeChart chart = new JFreeChart(plot);
		
		// By default, use the SWT native background color.
		RGB bgRGB = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB();
		chart.setBackgroundPaint(new Color(bgRGB.red, bgRGB.green, bgRGB.blue));
		
		setChart(chart);
		
		applyRenderer();
	}
	
	protected IntervalXYDataset createDataset(IDataProvider<E> dataProvider) {
		YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		
		for (int seriesIndex = 0; seriesIndex < dataProvider.getSeriesCount(); seriesIndex++) {
			String seriesName = dataProvider.getSeriesName(seriesIndex);
			List<E> items = dataProvider.buildSeries(seriesIndex);
			
			YIntervalSeries series = new YIntervalSeries(seriesName);
			int i=1;
			for (E item: items) {
				double[] value = dataProvider.getValue(item, new String[]{seriesName}, i);
				series.add(i, value[0], value[1], value[2]);
				i++;
			}
			
			dataset.addSeries(series);
		}
		return dataset;
	}

	private void applyRenderer() {
		if (getChart() == null) return;
		XYPlot plot = (XYPlot)getChart().getPlot();
		DeviationRenderer renderer = new DeviationRenderer(true, true);
		if (renderCustomizer != null) renderCustomizer.customize(renderer);
		plot.setRenderer(renderer);
		forceRedraw();
	}
}
