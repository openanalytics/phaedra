package eu.openanalytics.phaedra.ui.plate.chart.jfreechart;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.RectangleInsets;

import eu.openanalytics.phaedra.base.ui.charting.data.IDataProviderWPrintSupport;
import eu.openanalytics.phaedra.base.ui.charting.render.IRenderCustomizer;
import eu.openanalytics.phaedra.base.ui.charting.select.ChartSelectionListener;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.toolitem.DropdownToolItemFactory;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.ui.plate.chart.jfreechart.data.ExperimentTrendControlDataProvider;

public class CombinedTrendChart<E> extends ChartComposite {

	private static final String CONTROLS = "CONTROLS";
	private static final String STATISTICS = "STATISTICS";
	private static final String AUTO_RANGE = "AUTO_RANGE";

	private List<String> currentVisibleControls;
	private String currentVisibleStatistics;
	private ToolItem controlItems = null;
	private ToolItem statisticsItems = null;

	private IDataProviderWPrintSupport<E> controlDataProvider;
	private IDataProviderWPrintSupport<E> statisticDataProvider;
	private IRenderCustomizer controlRenderCustomizer;
	private IRenderCustomizer statisticRenderCustomizer;
	private XYPlot plot;

	private Point selectionPoint;
	private Rectangle selectionRectangle;

	private List<E> entities;
	private Set<E> selectedEntities;
	private ChartSelectionListener<E> selectionListener;
	private boolean isSelectionMode;

	public CombinedTrendChart(Composite comp){
		super(comp, SWT.NONE, null, 400, 300, 50, 50, 1500, 1500, true, false, false, false, false, false);
		setDefaults();
	}

	public CombinedTrendChart(Composite comp, int style) {
		super(comp, style, null, 400, 300, 50, 50, 1500, 1500, true, false, false, false, false, false);
		setDefaults();
	}

	private void setDefaults(){
		this.entities = Collections.emptyList();
		this.selectedEntities = new HashSet<>();
		this.isSelectionMode = true;

		//Default Settings
		currentVisibleControls = new ArrayList<String>();
		currentVisibleControls.add("LC");
		currentVisibleControls.add("HC");

		currentVisibleStatistics = "Z-Prime";
	}

	public void setDataProviders(IDataProviderWPrintSupport<E> controlDataProvider, IDataProviderWPrintSupport<E> statisticDataProvider) {
		this.controlDataProvider = controlDataProvider;
		this.statisticDataProvider = statisticDataProvider;

		if (controlDataProvider instanceof ExperimentTrendControlDataProvider) {
			ExperimentTrendControlDataProvider trendControlDataProvider = (ExperimentTrendControlDataProvider) controlDataProvider;
			trendControlDataProvider.setVisibleControls(currentVisibleControls);
		}

		buildChart();
	}

	public void setRenderCustomizers(IRenderCustomizer controlRenderCustomizer, IRenderCustomizer statisticRenderCustomizer) {
		this.controlRenderCustomizer = controlRenderCustomizer;
		this.statisticRenderCustomizer = statisticRenderCustomizer;
	}

	public void buildChart() {
		// Build a data set.
		XYDataset controlDataset = createControlDataset(controlDataProvider);
		XYDataset statisticDataset = createStatisticDataset(statisticDataProvider);

		// Build a plot.
		plot = new XYPlot();
		plot.setOrientation(PlotOrientation.VERTICAL);

		plot.setDomainAxis(controlDataProvider.createAxis(0, null));
		plot.getDomainAxis().setLabel("Plate Sequence");
		plot.getDomainAxis().setVerticalTickLabels(true);

		entities = controlDataProvider.buildSeries(0);

		plot.getDomainAxis().setStandardTickUnits(new CustomTickUnit().createUnits());
		plot.getDomainAxis().setTickLabelInsets(new RectangleInsets(10, 0, 0, 0));
		plot.getDomainAxis().setTickLabelFont(new java.awt.Font("SansSerif", 0, 10));

		plot.setRangeAxis(0, controlDataProvider.createAxis(1, null));
		plot.setRangeAxis(1, statisticDataProvider.createAxis(1, null));

		plot.setDataset(0, controlDataset);
		plot.setDataset(1, statisticDataset);
		plot.mapDatasetToRangeAxis(1, 1);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
		JFreeChart chart = new JFreeChart(plot);

		// By default, use the SWT native background color.
		RGB bgRGB = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB();
		chart.setBackgroundPaint(new Color(bgRGB.red, bgRGB.green, bgRGB.blue));

		setChart(chart);
		setZoomable(!isSelectionMode);

		applyRenderer();
	}

	public XYPlot getPlot() {
		return plot;
	}

	@Override
	public void mouseDown(MouseEvent event) {
		if (!isDomainZoomable() && !isRangeZoomable()) {
			startSelection(event);
		}
		super.mouseDown(event);
	}

	@Override
	public void mouseMove(MouseEvent event) {
		if (this.selectionPoint != null) {
			updateSelection(event);
		}
		super.mouseMove(event);
	}

	@Override
	public void mouseUp(MouseEvent event) {
		if (this.selectionPoint != null) {
			finishSelection(event);
		}
		if (event.button == 3 && (event.stateMask & SWT.CTRL) != SWT.CTRL && getMenu() != null) {
			displayMenu(event);
		}
		this.selectionPoint = null;
		this.selectionRectangle = null;
		super.mouseUp(event);
	}

	@Override
	public void paintControl(PaintEvent e) {
		super.paintControl(e);
		if (this.selectionRectangle != null) {
			e.gc.drawRectangle(selectionRectangle);
		}
	}

	private void startSelection(MouseEvent event) {
		Rectangle scaledDataArea = getScreenDataArea(event.x, event.y);
		if (scaledDataArea == null) return;
		this.selectionPoint = getPointInRectangle(event.x, event.y, scaledDataArea);
		int x = getXChartCoordinate(event.x);
		int y = getYChartCoordinate(event.y);

		setAnchor(new Point2D.Double(x, y));
		getChart().setNotify(true);

		if ((event.stateMask & SWT.CTRL) != SWT.CTRL) selectedEntities.clear();
		EntityCollection entityCollection = getEntityCollection();
		if (entityCollection != null) {
			ChartEntity entity = entityCollection.getEntity(x, y);
			E item = getObjectEntity(entity);
			if (item != null) {
				if (!selectedEntities.remove(item)) selectedEntities.add(item);
			}
		}
	}

	private void updateSelection(MouseEvent event) {
		Rectangle scaledDataArea = getScreenDataArea(selectionPoint.x, selectionPoint.y);
		Point movingPoint = getPointInRectangle(event.x, event.y, scaledDataArea);
		this.selectionRectangle = new Rectangle(selectionPoint.x, selectionPoint.y
				, movingPoint.x - selectionPoint.x, movingPoint.y - selectionPoint.y);
	}

	private void finishSelection(MouseEvent event) {
		EntityCollection entityCollection = getEntityCollection();
		if (entityCollection != null && selectionRectangle != null) {
			int x = getXChartCoordinate(selectionRectangle.x);
			int y = getYChartCoordinate(selectionRectangle.y);

			java.awt.Rectangle rect = new java.awt.Rectangle(x, y, selectionRectangle.width, selectionRectangle.height);
			if (rect.width < 0) {
				rect.x += selectionRectangle.width;
				rect.width = Math.abs(selectionRectangle.width);
			}
			if (rect.height < 0) {
				rect.y += selectionRectangle.height;
				rect.height = Math.abs(selectionRectangle.height);
			}

			@SuppressWarnings("unchecked")
			Iterator<ChartEntity> iterator = entityCollection.iterator();
			while (iterator.hasNext()) {
				ChartEntity entity = iterator.next();
				if (rect.intersects(entity.getArea().getBounds())) {
					E item = getObjectEntity(entity);
					if (item != null) selectedEntities.add(item);
				}
			}
		}
		// Send out selection.
		if (selectionListener != null) selectionListener.selectionChanged(selectedEntities);
		getChart().setNotify(true);
	}

	private void displayMenu(MouseEvent event) {
		if (!selectedEntities.isEmpty()) {
			Point point = toDisplay(event.x, event.y);
			getMenu().setLocation(point.x, point.y);
			getMenu().setVisible(true);
		}
	}

	private int getXChartCoordinate(int x) {
		return (int) ((x - getClientArea().x) / getScaleX());
	}

	private int getYChartCoordinate(int y) {
		return (int) ((y - getClientArea().y) / getScaleY());
	}

	private E getObjectEntity(ChartEntity entity) {
		E object = null;
		if (isValidEntity(entity)) {
			int index = ((XYItemEntity) entity).getItem();
			object = entities.get(index);
		}
		return object;
	}

	private boolean isValidEntity(ChartEntity entity) {
		return entity instanceof XYItemEntity;
	}

	public void addChartSelectionListener(ChartSelectionListener<E> listener) {
		this.selectionListener = listener;
	}

	public Collection<E> getSelection() {
		return selectedEntities;
	}

	public void setSelection(Collection<E> selectedEntities) {
		this.selectedEntities = new HashSet<>(selectedEntities);
	}

	private IntervalXYDataset createControlDataset(IDataProviderWPrintSupport<E> dataProvider) {
		YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();

		int seriesCount = dataProvider.getSeriesCount();
		for (int seriesIndex=0; seriesIndex<seriesCount; seriesIndex++) {
			String seriesName = dataProvider.getSeriesName(seriesIndex);
			YIntervalSeries series = new YIntervalSeries(seriesName);
			if (currentVisibleControls.contains(seriesName)) {
				List<E> items = dataProvider.buildSeries(0);
				int i = 1;
				for (E item : items) {
					// seriesName <- currentVisibleStatistics
					String sigma = "3";
					double[] value = dataProvider.getValue(item, new String[] { seriesName, sigma }, i);

					if (!Double.isNaN(value[0]) && !Double.isNaN(value[1]) && !Double.isNaN(value[2])) {
						series.add(i, value[0], value[1], value[2]);
					}

					i++;
				}
				dataset.addSeries(series);
			}
		}

		fillControlsDropdown();
		return dataset;
	}

	private XYSeriesCollection createStatisticDataset(IDataProviderWPrintSupport<E> dataProvider) {
		XYSeriesCollection dataset = new XYSeriesCollection();

		String seriesName = currentVisibleStatistics;// dataProvider.getSeriesName(0);
		List<E> items = dataProvider.buildSeries(0);

		XYSeries series = new XYSeries(seriesName);
		int i = 1;
		for (E item : items) {
			double[] value = dataProvider.getValue(item, new String[] { currentVisibleStatistics }, i);
			series.add(i, value[0]);
			i++;
		}

		dataset.addSeries(series);
		fillStatisticsDropdown();
		return dataset;
	}

	private void applyRenderer() {
		if (getChart() == null) return;

		XYPlot plot = (XYPlot) getChart().getPlot();
		DeviationRenderer renderer = new DeviationRenderer(true, true);
		renderer.setAlpha(0.3f);
		if (controlRenderCustomizer != null)
			controlRenderCustomizer.customize(renderer);
		plot.setRenderer(renderer);

		XYBarRenderer renderer2 = new XYBarRenderer();
		if (statisticRenderCustomizer != null)
			statisticRenderCustomizer.customize(renderer2);
		plot.setRenderer(1, renderer2);

		forceRedraw();
	}

	public void createToolBarButtons(ToolBar parent) {
		ToolItem item = new ToolItem(parent, SWT.PUSH);
		item.addListener(SWT.Selection, e -> {
			isSelectionMode = !isSelectionMode;
			setZoomSelectionToolItem(item);
			setZoomable(!isSelectionMode);
		});
		setZoomSelectionToolItem(item);

		controlItems = DropdownToolItemFactory.createDropdown(parent);
		controlItems.setImage(IconManager.getIconImage("chart_line.png"));
		controlItems.setToolTipText("Choose the plate controls to show in chart");

		statisticsItems = DropdownToolItemFactory.createDropdown(parent);
		statisticsItems.setImage(IconManager.getIconImage("aggregation.gif"));
		statisticsItems.setToolTipText("Choose the plate statistics to show in chart");
		
		fillControlsDropdown();
		fillStatisticsDropdown();
	}

	private void fillControlsDropdown() {
		if (controlItems == null) return;
		DropdownToolItemFactory.clearChildren(controlItems);

		if (controlDataProvider == null) return;

		Listener controlsListener = event -> {
			MenuItem selected = (MenuItem) event.widget;
			String param = selected.getText();
			if (currentVisibleControls.contains(param)) {
				currentVisibleControls.remove(param);
			} else {
				currentVisibleControls.add(param);
			}
			buildChart();
		};

		for (int i = 0; i < controlDataProvider.getSeriesCount(); i++) {
			String name = controlDataProvider.getSeriesName(i);
			MenuItem item = DropdownToolItemFactory.createChild(controlItems, name, SWT.CHECK);
			item.addListener(SWT.Selection, controlsListener);

			item.setSelection(currentVisibleControls.contains(name));
		}
	}

	private void fillStatisticsDropdown() {
		if (statisticsItems == null) return;
		DropdownToolItemFactory.clearChildren(statisticsItems);

		if (statisticDataProvider == null) return;

		Listener controlsListener = event -> {
			MenuItem selected = (MenuItem) event.widget;
			if (!selected.getSelection())
				return;
			String param = selected.getText();
			currentVisibleStatistics = param;
			buildChart();
		};

		String[] parameters = statisticDataProvider.getParameters();
		for (String param : parameters) {
			MenuItem item = DropdownToolItemFactory.createChild(statisticsItems, param, SWT.RADIO);
			item.addListener(SWT.Selection, controlsListener);
			if ((currentVisibleStatistics != null) && (currentVisibleStatistics.equals(param)))
				item.setSelection(true);
		}
	}

	public Properties getProperties() {
		Properties properties = new Properties();
		properties.addProperty(CONTROLS, currentVisibleControls);
		properties.addProperty(STATISTICS, currentVisibleStatistics);
		properties.addProperty(AUTO_RANGE, getPlot().getRangeAxis(1).isAutoRange());
		return properties;
	}

	public void setProperties(Properties properties) {
		currentVisibleControls = properties.getProperty(CONTROLS, currentVisibleControls);
		currentVisibleStatistics = properties.getProperty(STATISTICS, currentVisibleStatistics);
		if (plot != null) plot.getRangeAxis(1).setAutoRange(properties.getProperty(AUTO_RANGE, true));
		if (controlDataProvider != null && statisticDataProvider != null) buildChart();
	}

	private Point getPointInRectangle(int x, int y, Rectangle area) {
		x = Math.max(area.x, Math.min(x, area.x + area.width));
		y = Math.max(area.y, Math.min(y, area.y + area.height));
		return new Point(x, y);
	}

	private void setZoomable(boolean isZoomable) {
		setRangeZoomable(isZoomable);
		setDomainZoomable(isZoomable);
	}

	private void setZoomSelectionToolItem(ToolItem item) {
		if (isSelectionMode) {
			item.setImage(IconManager.getIconImage("select_lasso.png"));
			item.setToolTipText("Current mode : drag to select");
		} else {
			item.setImage(IconManager.getIconImage("zoom.png"));
			item.setToolTipText("Current mode : drag to zoom in");
		}
	}

	private EntityCollection getEntityCollection() {
		EntityCollection entityCollection = null;
		ChartRenderingInfo info = getChartRenderingInfo();
		if (info != null) entityCollection = info.getEntityCollection();
		return entityCollection;
	}

	private class CustomTickUnit extends NumberTickUnit {

		private static final long serialVersionUID = 1238056193376820890L;

		public CustomTickUnit() {
			super(1);
		}

		@Override
		public String valueToString(double value) {
			int count = entities.size();
			int labelSize = plot.getDomainAxis().getLabelFont().getSize() + 2;
			int availableSize = CombinedTrendChart.this.getSize().x;
			int skip = (int)((count*labelSize)/(double)availableSize);

			int index = (int)value - 1;
			if (skip > 0 && index % (skip+1) != 0) return "";
			E item = null;
			if (index >= 0 && index < entities.size()) item = entities.get(index);
			if (item == null) return "";
			return controlDataProvider.getLabel(item);
		}

		public TickUnits createUnits() {
			TickUnits units = new TickUnits();
			units.add(this);
			return units;
		}
	}

}
