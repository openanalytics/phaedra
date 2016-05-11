package eu.openanalytics.phaedra.base.ui.charting.v2.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.BitSet;
import java.util.List;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.batik.svggen.SVGGraphics2D;
import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes1DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.histogram.Histogram1DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.line.Line2DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataCalculator;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.SimplePlotData;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseChartView;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.plot.Points;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotEvent;
import uk.ac.starlink.ttools.plot.PlotListener;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.TablePlot;

public abstract class AbstractChart<ENTITY, ITEM> extends ComponentAdapter {

	private AbstractChartLayer<ENTITY, ITEM> layer;
	private ChartType type;
	private ChartName name;
	private TablePlot plot;
	private PlotState plotState;
	protected JPanel chartComponent;
	private BufferedImage plotImage;
	private boolean preserveAxesSpace = true;

	public static enum ChartName {
		AXES_DYNAMIC("Axes"), AXES_2D("Axes 2D"), AXES_1D("Axes 1D")
		, SELECTION("Selection"), GATES("Gates"), GATES_1D("Gates 1D"), CELL_IMAGE("Cell Image"), CELL_IMAGE_3D("Cell Image 3D")
		, HISTOGRAM_1D("Histogram 1D"), DENSITY_R_1D("Density R 1D"), DENSITY_1D("Density 1D"), DENSITY_2D("Density 2D")
		, LINE_2D("Line 2D"), SCATTER_2D("Scatter 2D"), SCATTER_3D("Scatter 3D"), PARALLEL_COORDINATES("Parallel Coordinates")
		, CONTOUR_2D("Contour 2D"), SCATTER_DENSITY_2D("Scatter Density 2D"), COMPOUND("Compound"), COMPOUND_3D("Compound 3D")
		, WELL_IMAGE("Well Image"), SILO_IMAGE("Image"), SILO_IMAGE_3D("Image 3D"), CONTOUR_3D("Contour 3D"), SCATTER_DENSITY_3D("Scatter Density 3D");

		private String decription;

		private ChartName(String decription) {
			this.decription = decription;
		}

		public String getDecription() {
			return decription;
		}

		public static ChartName getByDescription(String text) {
			for (ChartName name : values()) {
				if (name.getDecription().equalsIgnoreCase(text)) {
					return name;
				}
			}
			return null;
		}
	}

	public static enum ChartType {
		NONE(0), ONE_DIMENSIONAL(1), TWO_DIMENSIONAL(2), THREE_DIMENSIONAL(3), DYNAMIC(-1);
		private int numberOfDimensions;

		private ChartType(int numberOfDimensions) {
			this.numberOfDimensions = numberOfDimensions;
		}

		public int getNumberOfDimensions() {
			return numberOfDimensions;
		}

	}

	public void setSelection(BitSet bitSet) {
		// Do nothing.
	}

	public Component build() {
		chartComponent = new JPanel() {
			private static final long serialVersionUID = -4466990886695870499L;

			/**
			 * we construct the plotImage in a separate method call since this
			 * paintComponent thing is called multiple times while the image
			 * doesn't change
			 */
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				if (plotImage != null) {
					g.drawImage(plotImage, 0, 0, null);
				}
			}
		};
		chartComponent.setOpaque(false);
		chartComponent.addComponentListener(this);
		initializePlot(true);
		return chartComponent;
	}

	public PlotData createPlotData() {

		StarTable table = getDataProvider().generateStarTable();
		SimplePlotData plotData = new SimplePlotData(null, null, table, table.getColumnCount());
		Points points = null;
		try {
			points = plotData.readPoints(new DefaultBoundedRangeModel());
		} catch (IOException | InterruptedException e) {
			Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage()));
		}
		plotData.setPoints(points);
		plotData.setSubsets(getDataProvider().performGrouping());

		return plotData;
	}

	public Style[] getStyles() {
		return getDataProvider().getStyles(getChartSettings());
	}

	public abstract TablePlot createPlot();

	public abstract void updatePlotData();

	public synchronized void dataChanged() {
		// If there is no data, there is nothing to update.
		if (getDataProvider() == null || getDataProvider().getCurrentItems() == null || getDataProvider().getCurrentItems().isEmpty()) return;
		updatePlotData();
		settingsChanged();
	}

	public void settingsChanged() {
		if (getPlotState() != null) {
			((SimplePlotData) getPlotState().getPlotData()).setStyles(getStyles());
			repaint();
		}
	}

	public void repaint() {
		if (chartComponent != null) {
			SwingUtilities.invokeLater(() -> {
				plotImage = getPlotImage();
				getChartComponent().repaint();
			});
		}
	}

	public void hide() {
		if (getChartComponent() != null) {
			getChartComponent().setVisible(false);
		}
	}

	public void show() {
		if (getChartComponent() != null) {
			getChartComponent().setVisible(true);
		}
	}

	/**
	 * Get the plot image as a {@link BufferedImage}.
	 *
	 * @return Image of the plot
	 */
	public BufferedImage getPlotImage() {
		if (getChartComponent().getWidth() != 0 && getChartComponent().getHeight() != 0) {
			return getPlotImage(getChartComponent().getWidth(), getChartComponent().getHeight());
		}

		return null;
	}

	public BufferedImage getPlotImage(int width, int height) {
		boolean isBackgroundTransparant = getChartSettings().isBackgroundTransparant();

		BufferedImage img = null;
		if (isBackgroundTransparant) {
			img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		} else {
			img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}

		Graphics2D g = img.createGraphics();
		if (!isBackgroundTransparant) {
			g.setBackground(getChartSettings().getBackgroundColor());
			g.clearRect(0, 0, width, height);
		}

		drawPlotVectorImage(width, height, g);

		return img;
	}

	/**
	 * Draw this layer on top of the given BufferedImage object.
	 *
	 * @param width
	 *            The width (should be the width of the SVG)
	 * @param height
	 *            The height (should be the height of the SVG)
	 * @param img
	 *            The BufferedImage Object on which you want to draw this layer
	 */
	public void drawPlotVectorImage(int width, int height, BufferedImage img) {
		if (plot == null) return;

		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0x00ffffff, true));

		drawPlotVectorImage(width, height, g);

		g.dispose();
	}

	/**
	 * Draw this layer on top of the given {@link Graphics2D} object. Also supports {@link SVGGraphics2D}.
	 *
	 * @param width
	 *            The width (should be the width of the SVG)
	 * @param height
	 *            The height (should be the height of the SVG)
	 * @param g
	 *            The graphics object on which you want to draw this layer
	 */
	public void drawPlotVectorImage(int width, int height, Graphics2D g) {
		if (plot == null) return;

		JFrame frame = null;
		try {
			frame = new JFrame();
			frame.setUndecorated(true);
			frame.setPreferredSize(new Dimension(width, height));
			frame.add(plot);
			frame.pack();
			frame.setVisible(false);

			plot.paint(g);
		} finally {
			if (frame != null) frame.dispose();
		}
	}

	/* calculate bounds + padding */
	public double[][] getRangeBounds() {
		double[][] rangeBounds = getDataProvider().getDataBounds();
		double[][] topCatBounds = new double[rangeBounds.length][2];
		for (int dimension = 0; dimension < rangeBounds.length; dimension++) {
			double[] bounds = rangeBounds[dimension];
			if (bounds[0] >= Float.MAX_VALUE || bounds[0] == Double.MIN_VALUE || Double.isInfinite(bounds[0])) {
				bounds[0] = 0;
			}
			if (bounds[1] <= -Float.MAX_VALUE || bounds[1] == Double.MIN_VALUE || Double.isInfinite(bounds[1])) {
				bounds[1] = 0;
			}
			Range range = new Range();
			range.limit(rangeBounds[dimension]);
			range.pad(0.02);
			if (layer.getChart() instanceof Histogram1DChart || layer.getChart() instanceof Line2DChart || layer.getChart() instanceof Axes1DChart) {
				// Remove padding on the zero
				if (rangeBounds[dimension][0] == 0.0) {
					range.limit(0, range.getBounds()[1]);
				}
			}

			topCatBounds[dimension] = range.getFiniteBounds(getPlotState().getLogFlags()[dimension]);

			// Apply aux settings
			if (dimension >= getDataProvider().getSelectedFeatures().size()
					&& getDataProvider().getAuxiliaryFeatures().size() != 0) {
				int auxDimension = dimension - getDataProvider().getSelectedFeatures().size();
				List<AuxiliaryChartSettings> auxSettings = getChartSettings().getAuxiliaryChartSettings();
				AuxiliaryChartSettings auxSetting = auxSettings.get(auxDimension);
				double boundRange = topCatBounds[dimension][1] - topCatBounds[dimension][0];
				double[] newBounds = new double[2];
				newBounds[0] = topCatBounds[dimension][0] + boundRange * auxSetting.getLoCut();
				newBounds[1] = topCatBounds[dimension][0] + boundRange * auxSetting.getHiCut();
				topCatBounds[dimension] = newBounds;
			}
		}

		return topCatBounds;
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		repaint();
	}

	/* getters and setters */
	public boolean isStackable() {
		return false;
	}

	public ChartType getType() {
		return type;
	}

	public void setType(ChartType type) {
		this.type = type;
	}

	public AbstractChartLayer<ENTITY, ITEM> getLayer() {
		return layer;
	}

	public void setLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		this.layer = layer;
	}

	public IDataProvider<ENTITY, ITEM> getDataProvider() {
		return getLayer().getDataProvider();
	}

	public PlotState getPlotState() {
		return plotState;
	}

	public void setPlotState(PlotState plotState) {
		this.plotState = plotState;
	}

	public Component getChartComponent() {
		return chartComponent;
	}

	public TablePlot getPlot() {
		return plot;
	}

	public ChartSettings getChartSettings() {
		return getLayer().getChartSettings();
	}

	public BitSet getActiveSelection() {
		return null;
	}

	public BitSet getSelection(Shape shape, boolean isSingleSelection) {
		return null;
	}

	public boolean isPreserveAxesSpace() {
		return preserveAxesSpace;
	}

	public void setPreserveAxesSpace(boolean preserveAxesSpace) {
		this.preserveAxesSpace = preserveAxesSpace;
	}

	public ChartName getName() {
		return name;
	}

	public void setName(ChartName name) {
		this.name = name;
	}

	public abstract boolean isSupportSVG();

	/**
	 * If a feature has String values to display instead of numeric values, use
	 * them on the corresponding axis.
	 *
	 * Only works on charts with a PtPlotSurface, such as scatter plots.
	 */
	protected void applyAxisValueLabels(PtPlotSurface surface) {
		if (surface == null)
			return;

		surface.clear(true);

		List<String> selectedFeatures = getDataProvider().getSelectedFeatures();
		for (int i = 0; i < selectedFeatures.size(); i++) {
			String feature = selectedFeatures.get(i);
			String[] labels = getDataProvider().getAxisValueLabels(feature);
			if (labels != null) {
				// Replace numeric ticks with String ticks.
				for (int j = 0; j < labels.length; j++) {
					String label = labels[j];
					if (i == 0)
						surface.addXTick(label, j);
					else
						surface.addYTick(label, j);
				}
			}
		}
	}

	public void initializePlot(boolean interactive) {
		this.plot = createPlot();
		if (interactive) {
			plot.addPlotListener(new PlotListener() {
				@Override
				public void plotChanged(final PlotEvent evt) {
					getDataProvider().setAvailableNumberOfPoints(evt.getPotentialPointCount());
					getDataProvider().setSelectedNumberOfPoints(evt.getIncludedPointCount());
					getDataProvider().setVisibleNumberOfPoints(evt.getVisiblePointCount());
					// This triggers the Legend Update so the Total, Sel and Vis are updated.
					getDataProvider().getDataChangedObservable().valueChanged();
				}
			});
		}
		plot.setOpaque(false);
	}

	public IDataCalculator<ENTITY, ITEM> getDataCalculator() {
		return null;
	}

	public MouseAdapter getDragListener(BaseChartView<ENTITY, ITEM> chartView) {
		return new DragListener(chartView);
	}

	private class DragListener extends MouseAdapter implements MouseMotionListener {
		private java.awt.Point destination;
		private java.awt.Point origin;
		private double xMultiplier = 1;
		private double yMultiplier = 1;
		private double[][] bounds;
		private boolean isDragging;
		private BaseChartView<ENTITY, ITEM> chartView;

		public DragListener(BaseChartView<ENTITY, ITEM> chartView) {
			this.chartView = chartView;
		}

		@Override
		public void mousePressed(MouseEvent evt) {
			origin = evt.getLocationOnScreen();
			bounds = getDataProvider().getDataBounds();
			xMultiplier = (bounds[0][0] - bounds[0][1]) / getChartComponent().getWidth();
			yMultiplier = (bounds[1][0] - bounds[1][1]) / getChartComponent().getHeight();
		}

		@Override
		public void mouseDragged(MouseEvent evt) {
			destination = evt.getLocationOnScreen();
			double xdiff = (destination.x - origin.x) * xMultiplier;
			double ydiff = (destination.y - origin.y) * yMultiplier;

			bounds[0][0] = bounds[0][0] + xdiff;
			bounds[0][1] = bounds[0][1] + xdiff;
			// Keep the bounds on zero
			if (bounds[1][0] != 0) {
				bounds[1][0] = bounds[1][0] - ydiff;
			}
			bounds[1][1] = bounds[1][1] - ydiff;

			origin = destination;

			isDragging = true;
			chartView.setRedraw(false);
			for (AbstractChartLayer<ENTITY, ITEM> layer : chartView.getChartLayers()) {
				if (!layer.isSelectionLayer()) {
					layer.getChart().getPlotState().setDragging(isDragging);
					layer.getChart().getPlotState().setRanges(bounds);
					layer.getChart().settingsChanged();
				}
			}
			chartView.setRedraw(true);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (isDragging) {
				isDragging = false;
				for (AbstractChartLayer<ENTITY, ITEM> layer : chartView.getChartLayers()) {
					if (!layer.isSelectionLayer()) {
						layer.getChart().getChartComponent().setEnabled(false);
						layer.getChart().getPlotState().setDragging(isDragging);
						layer.getChart().getPlot().getState().setDragging(isDragging);
						layer.getChart().settingsChanged();
						layer.getChart().getChartComponent().setEnabled(true);
					}
				}
			}
		}

	}

}