package eu.openanalytics.phaedra.base.ui.charting.widget;

import java.awt.Color;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.experimental.chart.swt.ChartComposite;

import eu.openanalytics.phaedra.base.ui.charting.data.IDataProvider;

public abstract class BaseCategoryChart<E> extends ChartComposite {

	protected IDataProvider<E> dataProvider;

	public BaseCategoryChart(Composite comp, int style) {
		this(comp, style, false);
	}

	public BaseCategoryChart(Composite comp, int style, boolean tooltips) {
		super(
				comp, style, null,
				400, 300, 50, 50, 1500, 1500,
				true, false, false, false, false, tooltips);
	}

	public void setDataProvider(IDataProvider<E> dataProvider) {
		this.dataProvider = dataProvider;
		buildChart();
	}

	protected void buildChart() {

		if (dataProvider == null) return;

		// Build a dataset.
		CategoryDataset dataset = createDataset(dataProvider);

		// Build a plot.
		Plot plot = createPlot(dataset);

		JFreeChart chart = new JFreeChart(plot);

		// By default, use the SWT native background color.
		RGB bgRGB = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB();
		chart.setBackgroundPaint(new Color(bgRGB.red, bgRGB.green, bgRGB.blue));

		setChart(chart);
		applyRenderer();
		forceRedraw();
	}

	protected abstract CategoryDataset createDataset(IDataProvider<E> dataProvider);

	protected abstract Plot createPlot(CategoryDataset dataset);

	protected void applyRenderer() {
		if (getChart() == null) return;
	}

}
