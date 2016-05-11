package eu.openanalytics.phaedra.base.ui.charting.v2.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter3DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.selection.SelectionChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IJEPAwareDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.TopcatViewStyles;
import eu.openanalytics.phaedra.base.ui.icons.Activator;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import uk.ac.starlink.topcat.plot.SurfaceZoomRegionList;
import uk.ac.starlink.topcat.plot.Zoomer;
import uk.ac.starlink.ttools.plot.ParallelCoordPlot;
import uk.ac.starlink.ttools.plot.Plot3DState;
import uk.ac.starlink.ttools.plot.SurfacePlot;
import uk.ac.starlink.ttools.plot.TablePlot;

public class InteractiveChartView<ENTITY, ITEM> extends BaseChartView<ENTITY, ITEM> implements MouseWheelListener {

	private JPanel chartArea;
	private Frame chartFrame;
	private boolean showSelectedOnly = false;
	private MouseMode mouseMode = MouseMode.SELECT;
	private Zoomer zoomer;
	private boolean zoomed = false;
	private static final double CLICK_ZOOM_UNIT = 0.1;
	private MouseAdapter mover;
	private MouseAdapter blobSelectionListener;
	private BitSet activeSelection;
	private AbstractChartLayer<ENTITY, ITEM> activeLayer;

	private String chartTitle;
	private String[] chartLabels;

	private ValueObservable selectionChangedObservable = new ValueObservable();
	private ValueObservable dataChangedObservable = new ValueObservable();
	private ValueObservable activatedObservable = new ValueObservable();

	private Job scrollJob;

	public InteractiveChartView(final Composite parent, List<AbstractChartLayer<ENTITY, ITEM>> layers) {
		super(layers);

		chartFrame = SWT_AWT.new_Frame(parent);

		JRootPane root = new JRootPane();
		chartArea = new JPanel();
		chartArea.setOpaque(false);
		chartArea.setLayout(new OverlayLayout(chartArea));
		chartArea.setPreferredSize(chartFrame.getSize());
		root.getContentPane().add(chartArea);
		root.getContentPane().setBackground(Color.WHITE);

		try {
			SwingUtilities.invokeAndWait(() -> {
				// Order from high (top) -> low (bottom), therefore reverse them
				for (int i = getChartLayers().size() - 1; i >= 0; i--) {
					initializeLayer(getChartLayers().get(i));
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			EclipseLog.error(e.getMessage(), e, eu.openanalytics.phaedra.base.ui.charting.Activator.getDefault());
		}

		chartFrame.add(root.getContentPane());
		chartFrame.setCursor(mouseMode.getCursor());
	}

	public void initializeLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		Component chartPanel = layer.buildChartPanel();
		chartPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				Display.getDefault().asyncExec(() -> activatedObservable.valueChanged(e));
			}
		});
		chartPanel.setSize(chartArea.getSize());
		chartArea.add(chartPanel);
	}

	public static enum MouseMode {
		SELECT(0, IconManager.getIconImage("select_lasso.png"), new Cursor(Cursor.CROSSHAIR_CURSOR),
				"Current mode : drag to select items"), ZOOM(1, IconManager.getIconImage("zoom.png"), getZoomCursor(),
						"Current mode : drag to zoom in"), MOVE(2, IconManager.getIconImage("panning.png"), new Cursor(
								Cursor.MOVE_CURSOR), "Current mode : move for panning"), ROTATE(2, IconManager
										.getIconImage("rotate.png"), new Cursor(Cursor.MOVE_CURSOR), "Current mode : move to rotate");

		private int id;
		private Image image;
		private Cursor cursor;
		private String tooltip;

		private MouseMode(int id, Image image, Cursor cursor, String tooltip) {
			this.id = id;
			this.image = image;
			this.cursor = cursor;
			this.tooltip = tooltip;
		}

		public static MouseMode valueOf(int id, int dim) {
			for (MouseMode mode : values()) {
				if (mode.getId() == id) {
					if (mode.getId() == 2 && dim == ChartType.THREE_DIMENSIONAL.getNumberOfDimensions()) {
						return MouseMode.ROTATE;
					}
					return mode;
				}
			}
			throw new RuntimeException("Invalid id for mousemode" + id);
		}

		public int getId() {
			return id;
		}

		public Image getImage() {
			return image;
		}

		public Cursor getCursor() {
			return cursor;
		}

		public String getTooltip() {
			return tooltip;
		}
	}

	public void setShowSelectedOnly(boolean showSelectedOnly) {
		this.showSelectedOnly = showSelectedOnly;

		// Store selection in chart settings
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
			layer.getChartSettings().setShowSelectedOnly(showSelectedOnly);
			settingsChangedForLayer(layer);
		}
	}

	public boolean isShowSelectedOnly() {
		return showSelectedOnly;
	}

	public void resetZoom() {
		AbstractChart<ENTITY, ITEM> chart = getBottomEnabledLayer().getChart();
		if (chart.getType().getNumberOfDimensions() <= 2) {
			recalculateDataBounds();
		} else {
			// For the 3D Chart we also reset rotation. Has to be done for every chart.
			for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
				if (!layer.isSelectionLayer()) {
					Scatter3DChart<ENTITY, ITEM> chart3D = (Scatter3DChart<ENTITY, ITEM>) layer.getChart();
					chart3D.setZoom(1.0);
					chart3D.setRotation(TopcatViewStyles.getView(TopcatViewStyles.DEFAULT));
					chart3D.settingsChanged();
				}
			}
			updateSelection();
		}
		zoomed = false;
		initializeMouseMode();
	}

	@Override
	public void addChartLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		if (layer.isDataLayer()) {
			if (chartTitle != null) {
				layer.getDataProvider().setTitle(chartTitle);
			}
			layer.getDataProvider().getTitleChangedObservable().addObserver(createTitleChangedObserver());
			if (chartLabels != null) {
				layer.getDataProvider().setCustomAxisLabels(chartLabels);
			}
			layer.getDataProvider().getLabelChangedObservable().addObserver(createLabelChangedObserver());
		}
		layer.getChartSettings().setShowSelectedOnly(showSelectedOnly);
		super.addChartLayer(layer);
	}

	public void reloadDataForAllLayers(List<ITEM> entities) {
		reloadDataForAllLayers(entities, new NullProgressMonitor());
	}

	@Override
	public void reloadDataForAllLayers(List<ITEM> entities, IProgressMonitor monitor) {
		// Prevent concurrent modification when loading a Saved View before reloadDataForAllLayers is finished.
		synchronized (this) {
			super.reloadDataForAllLayers(entities, monitor);
		}

		if (monitor.isCanceled()) return;
		getDataChangedObservable().valueChanged();
	}

	@Override
	public void dataChangedForAllLayers() {
		super.dataChangedForAllLayers();

		initializeMouseMode();
	}

	public int getNumberOfDimensionsToBeShown() {
		int number = -1;
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
			if (layer.isDataLayer() && !layer.isAxesLayer() && !layer.isSelectionLayer()) {
				number = Math.max(layer.getChart().getType().getNumberOfDimensions(), number);
			}
		}
		return number;
	}

	public Map<String, List<String>> getFeaturesPerGroup() {
		AbstractChartLayer<ENTITY, ITEM> axesLayer = getBottomEnabledLayer();
		if (axesLayer != null && axesLayer.getDataProvider() != null
				&& axesLayer.getDataProvider().getFeatures() != null) {

			return axesLayer.getDataProvider().getFeaturesPerGroup();
		}
		return null;
	}

	public List<String> getSelectedFeatures() {
		AbstractChartLayer<ENTITY, ITEM> axesLayer = getFirstEnabledChartLayer();
		if (axesLayer != null) {
			List<String> features = axesLayer.getDataProvider().getSelectedFeatures();
			int dimensions = getNumberOfDimensionsToBeShown();
			if (features != null && !features.isEmpty()) {
				if (features.size() > dimensions && dimensions > 0) {
					return features.subList(0, dimensions);
				} else {
					return features;
				}
			}
		}
		return new ArrayList<>();
	}

	/**
	 * Returns JEP Expressions if applicable.
	 * @return JEP Expressions or null
	 */
	public String[] getJEPExpressions() {
		AbstractChartLayer<ENTITY, ITEM> axesLayer = getFirstEnabledChartLayer();
		if (axesLayer != null) {
			if (axesLayer.getDataProvider() instanceof IJEPAwareDataProvider) {
				IJEPAwareDataProvider dataprovider = (IJEPAwareDataProvider) axesLayer.getDataProvider();
				return dataprovider.getJepExpressions();
			}
		}
		return null;
	}

	public MouseMode getMouseMode() {
		return mouseMode;
	}

	public void toggleMouseMode() {
		removeZoomListener();
		removeMoveListener();
		removeSelectListener();

		mouseMode = MouseMode.valueOf((mouseMode.getId() + 1) % 3, getDimensionCount());
		chartFrame.setCursor(mouseMode.getCursor());

		addZoomListener();
		addMoveListener();
		addSelectListener();
	}

	public void initializeMouseMode() {
		removeZoomListener();
		removeMoveListener();
		removeSelectListener();
		addZoomListener();
		addMoveListener();
		addSelectListener();
	}

	@SuppressWarnings("unchecked")
	public Observer getActiveLayerChangedObserver() {
		return (o, arg) -> {
			AbstractChartLayer<ENTITY, ITEM> newActiveLayer = (AbstractChartLayer<ENTITY, ITEM>) arg;
			if (activeLayer != newActiveLayer) {
				activeLayer = newActiveLayer;
				// See if the axes and tooltip layer features differ from the selected layer, if so, change them as well.
				updateAxesLayer();
				dataChangedObservable.valueChanged();
			}
		};
	}

	public Observer getLayerOrderChangedObserver() {
		return (o, arg) -> {
			for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
				int zIndex = getChartLayers().size() - layer.getOrder();
				chartArea.setComponentZOrder(layer.getChart().getChartComponent(), zIndex);
			}

			// Refresh all layers
			dataChangedForAllLayers();
		};
	}

	@SuppressWarnings("unchecked")
	public Observer getLayerGroupingChangedObserver() {
		return (o, arg) -> {
			dataChangedForLayer((AbstractChartLayer<ENTITY, ITEM>) arg);
			recalculateDataBounds();
		};
	}

	public Observer getLayerStatusChangedObserver() {
		return (o, arg) -> dataChangedForAllLayers();
	}

	public Observer getLayerFilterChangedObserver() {
		return (o, arg) -> recalculateDataBounds();
	}

	@SuppressWarnings("unchecked")
	public Observer getLayerRemovedObserver() {
		return (o, arg) -> {
			if (arg instanceof AbstractChartLayer) {
				removeChartLayer((AbstractChartLayer<ENTITY, ITEM>) arg);
			}
		};
	}

	public Observer getLayerToggleObserver() {
		return (o, arg) -> {
			if (arg instanceof AbstractChartLayer) {
				AbstractChartLayer<?, ?> layer = (AbstractChartLayer<?, ?>) arg;
				AbstractChart<?, ?> chart = layer.getChart();
				chart.settingsChanged();
				chart.repaint();

				if (layer.isEnabled()) {
					layer.showChart();
				} else {
					layer.hideChart();
				}
			}
		};
	}

	public Observer getEntitySelectionChangedObserver() {
		return (o, arg) -> {
			if (arg instanceof List) {
				@SuppressWarnings("unchecked")
				List<ITEM> list = (List<ITEM>) arg;
				JobUtils.runUserJob(monitor -> {
					reloadDataForAllLayers(list, monitor);
				}, "Loading Chart Data", 100, toString() + "SELECTION", null);
			}
		};
	}

	public Observer getEntityHighlightChangedObserver() {
		return (o, arg) -> {
			if (arg instanceof List) {
				JobUtils.runUserJob(monitor -> {
					List<?> selection = (List<?>) arg;
					highlightForAllLayers(selection, monitor);
				}, "Applying Chart Selection", 100, toString() + "HIGHLIGHT", null);
			}
		};
	}

	public ValueObservable getSelectionChangedObservable() {
		return selectionChangedObservable;
	}

	public ValueObservable getActivatedObservable() {
		return activatedObservable;
	}

	public ValueObservable getDataChangedObservable() {
		return dataChangedObservable;
	}

	public void dispose() {
		JobUtils.cancelJobs(toString() + "SELECTION");
		JobUtils.cancelJobs(toString() + "HIGHLIGHT");
		SwingUtilities.invokeLater(chartFrame::dispose);
	}

	@Override
	public void mouseScrolled(org.eclipse.swt.events.MouseEvent e) {
		if (MouseMode.ZOOM.equals(getMouseMode())) {
			if (scrollJob == null) {
				scrollJob = new Job("Chart Repaint") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
							AbstractChart<ENTITY, ITEM> chart = layer.getChart();
							if (chart instanceof Scatter3DChart) {
								Plot3DState plot3dState = (Plot3DState) chart.getPlotState();
								plot3dState.setRotating(false);
								chart.settingsChanged();
							}
						}
						updateSelection();
						return Status.OK_STATUS;
					}
				};
			}

			int count = e.count;
			if (count != 0) {
				double zoomFactor = 1.0 + (count < 0 ? -CLICK_ZOOM_UNIT : CLICK_ZOOM_UNIT);
				boolean isNewZoom = true;
				double newZoom = 1f;

				for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
					AbstractChart<ENTITY, ITEM> chart = layer.getChart();
					if (chart instanceof Scatter3DChart) {
						Plot3DState plot3dState = (Plot3DState) chart.getPlotState();
						plot3dState.setRotating(true);
						if (isNewZoom) {
							// Only calculating the currentZoom once prevents different zoom values for other 3D layers.
							double currentZoom = plot3dState.getZoomScale();
							newZoom = currentZoom * zoomFactor;
							isNewZoom = false;
						}
						((Scatter3DChart<ENTITY, ITEM>) chart).setZoom(newZoom);
						chart.settingsChanged();
						scrollJob.cancel();
						scrollJob.schedule(500);
					}
				}
			}
		}
	}

	public void removeAllLayers() {
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
			chartArea.remove(layer.getChart().getChartComponent());
		}
		activeLayer = null;
		getChartLayers().clear();
	}

	@Override
	public void removeChartLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		super.removeChartLayer(layer);
		chartArea.remove(layer.getChart().getChartComponent());
		chartArea.repaint();
		initializeMouseMode();
	}

	@Override
	public void setRedraw(boolean isRedraw) {
		chartFrame.setEnabled(isRedraw);
	}

	public void setVisible(boolean isVisible) {
		chartFrame.setVisible(isVisible);
	}

	public boolean removeSelectionLayer() {
		AbstractChartLayer<ENTITY, ITEM> selectionLayer = getSelectionLayer();
		if (selectionLayer != null) {
			removeChartLayer(getSelectionLayer());
			return true;
		}
		return false;
	}

	/**
	 * Updates the selection of other layers to that of the active layer.
	 * It also creates the proper ISelection.
	 *
	 * This method is public for Contour (3D) chart because the plot itself can change selection by zooming/turning.
	 */
	public void updateSelection() {
		if (activeLayer == null) return;
		getSelectionChangedObservable().valueChanged(activeLayer.getDataProvider().createSelection(activeSelection));

		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
			if (layer.isDataLayer() && !layer.isAxesLayer()) {
				layer.getChart().setSelection(activeSelection);
				layer.getChart().repaint();
			}
		}
	}

	@Override
	protected void updateAxesLayer() {
		super.updateAxesLayer();

		initializeActiveLayer();
		for (AbstractChartLayer<ENTITY, ITEM> l : getChartLayers()) {
			if (l.isAxesLayer() || l.isTooltipLayer() || l.isGateLayer()) {
				boolean changed = false;
				if (!l.getDataProvider().getSelectedFeatures().equals(activeLayer.getDataProvider().getSelectedFeatures())) {
					l.getDataProvider().setSelectedFeatures(activeLayer.getDataProvider().getSelectedFeatures());
					changed = true;
				}
				if (l.getChartSettings().getStringMiscSetting(INDEPENDENT_LAYER) != null) {
					l.getDataProvider().setDataBounds(activeLayer.getDataProvider().getDataBounds());
					changed = true;
				}
				if (changed) dataChangedForLayer(l);
			}
		}
	}

	private void highlightForAllLayers(List<?> selection, IProgressMonitor monitor) {
		monitor.setTaskName("Loading Chart Selection");
		BitSet newSelection = null;
		synchronized (this) {
			List<AbstractChartLayer<ENTITY, ITEM>> chartLayers = getChartLayers();
			for (AbstractChartLayer<ENTITY, ITEM> layer : chartLayers) {
				IDataProvider<ENTITY, ITEM> dataProvider = layer.getDataProvider();
				if (dataProvider == null) continue;
				newSelection = dataProvider.createSelection(selection);
				break;
			}
		}
		monitor.worked(95);

		if (newSelection != null) {
			activeSelection = newSelection;
			for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
				if (!layer.isAxesLayer() && layer.getChart() != null) {
					layer.getChart().setSelection(activeSelection);
					layer.getChart().repaint();
				}
			}
		}
		monitor.worked(5);
		monitor.done();
	}

	private void addSelectListener() {
		if (MouseMode.SELECT.equals(mouseMode)) {
			AbstractChartLayer<ENTITY, ITEM> layer = getSelectionLayer();
			if (layer != null) {
				layer.setEnabled(true);
				layer.showChart();

				blobSelectionListener = new MouseAdapter() {
					@Override
					public void mouseReleased(MouseEvent e) {
						if (!e.isPopupTrigger()) {
							createSelection(e.isControlDown());
							((SelectionChart<ENTITY, ITEM>) getSelectionLayer().getChart()).clear();
						}
					}
				};
				layer.getChart().getChartComponent().addMouseListener(blobSelectionListener);
			}
		}
	}

	private void removeSelectListener() {
		AbstractChartLayer<ENTITY, ITEM> layer = getSelectionLayer();
		if (layer != null) {
			layer.setEnabled(false);
			layer.hideChart();
			layer.getChart().getChartComponent().removeMouseListener(blobSelectionListener);
		}
	}

	private Observer createTitleChangedObserver() {
		return (o, arg) -> {
			chartTitle = (String) arg;
			for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
				if (layer.isDataLayer()) {
					layer.getDataProvider().setTitle(chartTitle);
					layer.dataChanged();
					layer.settingsChanged();
				}
			}
		};
	}

	private Observer createLabelChangedObserver() {
		return (o, arg) -> {
			chartLabels = (String[]) arg;
			for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
				if (layer.isDataLayer()) {
					layer.getDataProvider().setCustomAxisLabels(chartLabels);
					if (layer.isGateLayer()) {
						layer.settingsChanged();
					}
				}
			}
		};
	}

	private void createSelection(boolean keepPreviousSelection) {
		initializeActiveLayer();

		// Get the selection on the selected layer
		if (activeLayer != null && activeLayer.getChart().getPlotState() != null) {
			activeSelection = activeLayer.getActiveSelection();
			BitSet newSelectionBitSet = activeLayer.getSelection(getSelectionLayer().getSelection(), getSelectionLayer().isSingleSelection());
			if (activeSelection == null || !keepPreviousSelection) {
				activeSelection = newSelectionBitSet;
			} else {
				if (activeSelection.intersects(newSelectionBitSet)) {
					// Remove re-selected points
					activeSelection.andNot(newSelectionBitSet);
				} else {
					activeSelection.or(newSelectionBitSet);
				}
			}

			updateSelection();
		}
	}

	private void initializeActiveLayer() {
		// By default select first layer if there is no active layer.
		if (activeLayer == null && getChartLayers().size() > 1) {
			activeLayer = getFirstEnabledChartLayer();
		}
	}

	/**
	 * Add ZoomListener if it is a scatter or density plot ZoomListener only
	 * reacts to zooming by dragging, not scrolling 3D zooming is done by scrolling
	 */
	private void addZoomListener() {
		if (MouseMode.ZOOM.equals(mouseMode)) {
			// Get the top layer
			final AbstractChartLayer<ENTITY, ITEM> layer = getTopEnabledLayer();

			if (layer.getChart().getType().getNumberOfDimensions() <= 2) {
				TablePlot plot = layer.getChart().getPlot();
				if (plot instanceof SurfacePlot) {
					SurfacePlot surfacePlot = (SurfacePlot) plot;
					final SurfaceZoomRegionList zoomRegionList;
					if (surfacePlot instanceof ParallelCoordPlot) {
						zoomRegionList = new SurfaceZoomRegionList(surfacePlot) {
							@Override
							protected void requestZoom(double[][] bounds) {
								double[][] originalBounds = layer.getDataProvider().getDataBounds();
								double pct0 = 1 - bounds[1][0];
								double pct1 = 1 - bounds[1][1];
								for (double[] range : originalBounds) {
									double value = range[1] - range[0];
									range[1] = range[0] + value * pct1;
									range[0] += value * pct0;
								}
								setDataBoundsForAllLayers(originalBounds);
								zoomed = true;
							}
						};
					} else {
						zoomRegionList = new SurfaceZoomRegionList(surfacePlot) {
							@Override
							protected void requestZoom(double[][] bounds) {
								// If the original bounds are 0 we would like to keep it that way for certain charts (e.g. Histogram) to prevent weird results.
								// The 1D Chart check was added because it would mess up zooming for charts which also have an original bounds of 0
								if (layer.getChart().getType().getNumberOfDimensions() == 1) {
									double[][] originalBounds = layer.getDataProvider().getDataBounds();
									if (originalBounds != null && originalBounds[1][0] == 0) {
										bounds[1][0] = 0;
									}
								}

								for (int i = 0; i < bounds.length; i++) {
									if (bounds[i] == null) {
										double[][] originalBounds = layer.getDataProvider().getDataBounds();
										if (originalBounds != null) bounds[i] = originalBounds[i];
									}
								}

								setDataBoundsForAllLayers(bounds);
								zoomed = true;
							}
						};
					}

					zoomer = new Zoomer();
					zoomer.setRegions(zoomRegionList);
					zoomer.setCursorComponent(layer.getChart().getChartComponent());
					layer.getChart().getChartComponent().addMouseListener(zoomer);
					layer.getChart().getChartComponent().addMouseMotionListener(zoomer);

					// Initial reconfigure
					zoomRegionList.reconfigure();

					surfacePlot.addPlotListener(e -> zoomRegionList.reconfigure());
				}
			}
		}
	}

	private void removeZoomListener() {
		if (zoomer != null) {
			for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
				layer.getChart().getChartComponent().removeMouseListener(zoomer);
				layer.getChart().getChartComponent().removeMouseMotionListener(zoomer);
			}
		}
	}

	private void addMoveListener() {
		if (MouseMode.MOVE.equals(mouseMode) || MouseMode.ROTATE.equals(mouseMode)) {
			AbstractChartLayer<ENTITY, ITEM> layer = getTopEnabledLayer();
			mover = null;
			if (layer.getChart().getType().getNumberOfDimensions() < 3) {
				if (zoomed) {
					mover = layer.getChart().getDragListener(this);
				}
			} else {
				mover = layer.getChart().getDragListener(this);
			}

			layer.getChart().getChartComponent().addMouseListener(mover);
			layer.getChart().getChartComponent().addMouseMotionListener(mover);
		}
	}

	private void removeMoveListener() {
		if (mover != null) {
			for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
				layer.getChart().getChartComponent().removeMouseListener(mover);
				layer.getChart().getChartComponent().removeMouseMotionListener(mover);
			}
		}
	}

	private static Cursor getZoomCursor() {
		// Create zoom cursor
		URL url = FileLocator.find(Activator.getDefault().getBundle(), new Path("icons/zoom_cursor.gif"), null);
		java.awt.Image zoomImage = null;
		int[] cursorHotSpot = new int[2];
		try {
			zoomImage = ImageIO.read(url);

			// Create a cursor image matching the systems preferred size.
			Dimension cursorDim = Toolkit.getDefaultToolkit().getBestCursorSize(0, 0);
			int imgW = zoomImage.getWidth(null);
			int imgH = zoomImage.getHeight(null);
			cursorHotSpot[0] = imgW / 2;
			cursorHotSpot[1] = imgH / 2;

			if (cursorDim.width != imgW || cursorDim.height != imgH) {
				java.awt.Image scaledImage = new BufferedImage(cursorDim.width, cursorDim.height,
						BufferedImage.TYPE_INT_ARGB);
				scaledImage.getGraphics().drawImage(zoomImage, 0, 0, imgW, imgH, 0, 0, imgW, imgH, null);
				zoomImage = scaledImage;
			}
		} catch (IOException e) {
			EclipseLog.error(e.getMessage(), e, eu.openanalytics.phaedra.base.ui.charting.Activator.getDefault());
		}
		return Toolkit.getDefaultToolkit().createCustomCursor(zoomImage,
				new java.awt.Point(cursorHotSpot[0], cursorHotSpot[1]), "CURSOR_ZOOM");
	}

}