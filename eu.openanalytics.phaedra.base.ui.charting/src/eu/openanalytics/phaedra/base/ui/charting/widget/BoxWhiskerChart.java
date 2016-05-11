package eu.openanalytics.phaedra.base.ui.charting.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import eu.openanalytics.phaedra.base.ui.charting.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.render.ICategoryRenderCustomizer;
import eu.openanalytics.phaedra.base.ui.charting.select.ChartSelectionListener;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class BoxWhiskerChart<E> extends BaseCategoryChart<E> {

	private CategoryPlot plot;
	private ICategoryRenderCustomizer renderCustomizer;

	private Point selectionPoint;
	private Rectangle selectionRectangle;

	private List<E> entities;
	private Set<E> selectedEntities;
	private ChartSelectionListener<E> selectionListener;
	private boolean isSelectionMode;

	public BoxWhiskerChart(Composite comp, int style) {
		super(comp, style, true);

		this.selectedEntities = new HashSet<>();
		this.isSelectionMode = true;
	}

	public void addChartSelectionListener(ChartSelectionListener<E> listener) {
		this.selectionListener = listener;
	}

	public void setRenderCustomizer(ICategoryRenderCustomizer renderCustomizer) {
		this.renderCustomizer = renderCustomizer;
	}

	public void fillToolBar(ToolBar parent) {
		ToolItem item = new ToolItem(parent, SWT.PUSH);
		item.addListener(SWT.Selection, e -> {
			isSelectionMode = !isSelectionMode;
			setZoomSelectionToolItem(item);
			setZoomable(!isSelectionMode);
		});
		setZoomSelectionToolItem(item);
	}

	@Override
	protected void buildChart() {
		super.buildChart();

		setZoomable(!isSelectionMode);
	}

	@Override
	protected Plot createPlot(CategoryDataset dataset) {
		JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
				null, null, null, (BoxAndWhiskerCategoryDataset)dataset, true);

		plot = (CategoryPlot) chart.getPlot();
		// Change axis color to black.
		plot.getRangeAxis().setTickLabelPaint(Color.BLACK);
		plot.getDomainAxis().setTickLabelPaint(Color.BLACK);
		plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", 0, 10));
		plot.getDomainAxis().setCategoryLabelPositionOffset(10);
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
		// Add tooltips to the domain axis (since labels will not always be visible).
		for (int i = 0; i < dataset.getColumnCount(); i++) {
			Comparable<?> columnKey = dataset.getColumnKey(i);
			plot.getDomainAxis().addCategoryLabelToolTip(columnKey, (String) columnKey);
		}
		// Change background to white, with gray grid lines.
		plot.setRangeGridlinePaint(Color.GRAY);
		plot.setBackgroundPaint(Color.WHITE);

		return plot;
	}

	@Override
	protected CategoryDataset createDataset(IDataProvider<E> dataProvider) {
		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i<dataProvider.getSeriesCount(); i++) {
			String seriesName = dataProvider.getSeriesName(i);
			entities = dataProvider.buildSeries(i);
			if (entities != null && !entities.isEmpty()) {
				for (E item: entities) {
					double[] value = dataProvider.getValue(item, new String[]{seriesName}, -1);
					if (value != null) {
						BoxAndWhiskerItem dataItem = new BoxAndWhiskerItem(
								value[0],value[1],value[2],value[3],
								value[4],value[5],value[6],value[7], null);
						dataset.add(dataItem, seriesName, dataProvider.getLabel(item));
					}
				}
			}
		}
		return dataset;
	}

	@Override
	protected void applyRenderer() {
		super.applyRenderer();
		CategoryPlot plot = (CategoryPlot)getChart().getPlot();
		BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		if (renderCustomizer != null) renderCustomizer.customize(renderer);
		plot.setRenderer(renderer);
		forceRedraw();
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

	public void setSelection(List<E> selectedEntities) {
		this.selectedEntities = new HashSet<>(selectedEntities);
		updateHighlight();
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

		updateHighlight();

		// Send out selection.
		if (selectionListener != null) selectionListener.selectionChanged(selectedEntities);
		getChart().setNotify(true);
	}

	private void updateHighlight() {
		plot.clearDomainMarkers();
		for (E entity : selectedEntities) {
			Comparable<?> columnKey = dataProvider.getLabel(entity);
			plot.addDomainMarker(new CategoryMarker(columnKey, Color.YELLOW, new BasicStroke(1f), null, null, .40f));
		}
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

	private Point getPointInRectangle(int x, int y, Rectangle area) {
		x = Math.max(area.x, Math.min(x, area.x + area.width));
		y = Math.max(area.y, Math.min(y, area.y + area.height));
		return new Point(x, y);
	}

	private EntityCollection getEntityCollection() {
		EntityCollection entityCollection = null;
		ChartRenderingInfo info = getChartRenderingInfo();
		if (info != null) entityCollection = info.getEntityCollection();
		return entityCollection;
	}

	private E getObjectEntity(ChartEntity entity) {
		E object = null;
		if (isValidEntity(entity)) {
			Comparable<?> columnKey = ((CategoryItemEntity) entity).getColumnKey();
			int index = plot.getDataset().getColumnIndex(columnKey);
			object = entities.get(index);
		}
		return object;
	}

	private boolean isValidEntity(ChartEntity entity) {
		return entity instanceof CategoryItemEntity;
	}

}
