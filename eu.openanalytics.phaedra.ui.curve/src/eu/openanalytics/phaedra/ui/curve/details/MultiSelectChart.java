package eu.openanalytics.phaedra.ui.curve.details;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.experimental.chart.swt.ChartComposite;

import eu.openanalytics.phaedra.base.util.misc.SWTUtils;

/**
 * NOTE: AWT mouse events go before SWT mouse events
 */
public class MultiSelectChart extends ChartComposite {

	private int dragMode;
	private boolean dragInProgress;

	private Point dragStart;
	private Point dragEnd;

	private ChartMouseListener chartMouseListener;
	private MouseListener mouseListener;
	private MouseMoveListener mouseMoveListener;

	private Cursor handCursor;
	private Cursor arrowCursor;

	private List<XYItemEntity> currentSelection;

	private static Color dragBGColor = new Color(null, new RGB(14, 200, 255));

	public final static int DRAG_MODE_ZOOM = 1;
	public final static int DRAG_MODE_SELECT = 2;

	public MultiSelectChart(Composite comp, int style, JFreeChart chart) {
		super(comp, style, null,50, 50, 50, 50, 2000, 2000,true, false, true, false, false, false);

		setDragMode(DRAG_MODE_SELECT);
		dragInProgress = false;

		handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		arrowCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW);

		chartMouseListener = new ChartMouseListener() {
			@Override
			public void chartMouseMoved(ChartMouseEvent event) {
				ChartEntity entity = event.getEntity();
				if (entity != null && entity instanceof XYItemEntity 
						&& dragMode == DRAG_MODE_SELECT) {
					setCursor(handCursor);
				} else {
					setCursor(arrowCursor);
				}
			}
			@Override
			public void chartMouseClicked(ChartMouseEvent event) {
				int btn = event.getTrigger().getButton();
				boolean rightBtn = btn == java.awt.event.MouseEvent.BUTTON3;
				if (rightBtn && (currentSelection == null || currentSelection.isEmpty())) {
					ChartEntity entity = event.getEntity();
					if (entity != null && entity instanceof XYItemEntity) {
						XYItemEntity item = (XYItemEntity)entity;
						List<XYItemEntity> selection = new ArrayList<XYItemEntity>();
						selection.add(item);
						handleSelection(selection);
					}
				}
			}
		};
		addChartMouseListener(chartMouseListener);

		mouseListener = new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				//Do nothing.
			}
			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == 1) {
					dragInProgress = true;
					dragStart = new Point(e.x, e.y);
					dragEnd = new Point(e.x, e.y);
				}
			}
			@Override
			public void mouseUp(MouseEvent e) {
				if (dragInProgress) {
					dragInProgress = false;

					if (dragMode == DRAG_MODE_SELECT) {
						int w = dragEnd.x-dragStart.x;
						int h = dragEnd.y-dragStart.y;
						Rectangle box = new Rectangle(dragStart.x, dragStart.y, w, h);
						SWTUtils.normalize(box);

						handleSelection(box);
						forceRedraw();
					}
				}
			}
		};
		addSWTListener(mouseListener);

		mouseMoveListener = new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if (dragInProgress) {
					dragEnd = new Point(e.x, e.y);
				}
			}
		};
		addSWTListener(mouseMoveListener);

		PaintListener paintListener = new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (dragInProgress) {
					int w = dragEnd.x-dragStart.x;
					int h = dragEnd.y-dragStart.y;
					Rectangle box = new Rectangle(dragStart.x, dragStart.y, w, h);

					GC g = e.gc;
					g.setBackground(dragBGColor);
					g.setAlpha(0x3f);
					g.fillRectangle(box);
				}
			}
		};
		addSWTListener(paintListener);
	}

	protected void handleSelection(Rectangle box) {
		List<XYItemEntity> selectedItems = new ArrayList<XYItemEntity>();
		EntityCollection col = getChartRenderingInfo().getEntityCollection();
		for (Object entity : col.getEntities()) {
			if (entity instanceof XYItemEntity) {
				XYItemEntity item = (XYItemEntity)entity;
				Rectangle2D shape = item.getArea().getBounds2D();
				Point shapeCenter = new Point((int)shape.getCenterX(), (int)shape.getCenterY());
				if (box.contains(shapeCenter)) {
					//TODO: Check dataset & series index dynamically. See CrcChartFactory.
					if (item.getDataset().getSeriesCount() > 2 && item.getSeriesIndex() < 2)
						selectedItems.add(item);
				} else if (box.width == 0 && box.height == 0) {
					// Single item click.
					if (shape.contains(new java.awt.Point(box.x, box.y))) {
						selectedItems.add(item);
						break;
					}
				}
			}
		}
		handleSelection(selectedItems);
	}

	public void handleSelection(List<XYItemEntity> selectedItems) {
		currentSelection = selectedItems;
		// pass selection to renderer.
		if (getChart()==null)
			return;
		
		XYPlot plot = (XYPlot)getChart().getPlot();
		XYItemRenderer renderer = plot.getRenderer();
		if (renderer instanceof MultiSelectRenderer) {
			MultiSelectRenderer msRenderer = (MultiSelectRenderer)renderer;
			List<int[]> selection = new ArrayList<int[]>();
			for (XYItemEntity item: selectedItems) {
				selection.add(new int[]{item.getSeriesIndex(),item.getItem()});
			}
			msRenderer.setSelection(selection);
		}
	}

	public void setChart(JFreeChart chart) {
		super.setChart(chart);
		setDragMode(getDragMode());
	}

	public List<XYItemEntity> getCurrentSelection() {
		return currentSelection;
	}

	public int getDragMode() {
		return dragMode;
	}

	public void setDragMode(int dragMode) {
		this.dragMode = dragMode;

		boolean zoomMode = dragMode == DRAG_MODE_ZOOM;
		setDomainZoomable(zoomMode);
		setRangeZoomable(zoomMode);
	}
}
