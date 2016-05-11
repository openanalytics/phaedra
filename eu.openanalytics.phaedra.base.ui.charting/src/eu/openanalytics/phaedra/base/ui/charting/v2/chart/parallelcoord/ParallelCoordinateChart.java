package eu.openanalytics.phaedra.base.ui.charting.v2.chart.parallelcoord;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.PARALLEL_COORDINATES;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType.DYNAMIC;

import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.BitSet;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseChartView;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.ParallelCoordDataPanel;
import uk.ac.starlink.ttools.plot.ParallelCoordPlot;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.TablePlot;

public class ParallelCoordinateChart<ENTITY, ITEM> extends AbstractChart<ENTITY, ITEM> {

	public ParallelCoordinateChart() {
		setType(DYNAMIC);
		setName(PARALLEL_COORDINATES);
	}

	@Override
	public TablePlot createPlot() {
		getChartSettings().setLines(true);
		ParallelCoordDataPanel surface = new ParallelCoordDataPanel();
		surface.setPreserveAxesSpace(isPreserveAxesSpace());
		return new ParallelCoordPlot(surface, true);
	}

	@Override
	public void updatePlotData() {
		if (getDataProvider() == null) {
			return;
		}

		/* Set per-axis characteristics. */
		int axesCount = getDataProvider().getSelectedFeatures().size();
		ColumnInfo[] axinfos = new ColumnInfo[axesCount];
		ValueConverter[] converters = new ValueConverter[axesCount];
		for (int i = 0; i < axesCount; i++) {
			int index = getDataProvider().getFeatureIndex(getDataProvider().getSelectedFeature(i));
			ColumnInfo cinfo = getDataProvider().getColumnInfo(index);
			axinfos[i] = cinfo;
			converters[i] = (ValueConverter) cinfo.getAuxDatumValue(TopcatUtils.NUMERIC_CONVERTER_INFO, ValueConverter.class);
		}
		PlotState state = new PlotState();
		setPlotState(state);
		state.setPlotData(createPlotData());

		state.setMainNdim(getType().getNumberOfDimensions());
		state.setAntialias(true);

		state.setAxes(axinfos);
		state.setConverters(converters);
		state.setLogFlags(new boolean[getDataProvider().getDimensionCount()]);
		state.setFlipFlags(new boolean[getDataProvider().getDimensionCount()]);
		state.setShowAxes(false);

		/* Set axis labels configured in the axis editor window. */
		state.setAxisLabels(getDataProvider().getAxisLabels());

		state.setRanges(getRangeBounds());
		state.setValid(true);
		getPlot().setState(state);
	}

	@Override
	public void setLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		super.setLayer(layer);
		// Set default selection transparency to 80% Parallel Coordinates Plot
		getChartSettings().setSelectionOpacity(80);
	}

	@Override
	public void settingsChanged() {
		((ParallelCoordPlot) getPlot()).setOpacity(getChartSettings().getOpacity());
		// Use fog variable for selection highlight
		((ParallelCoordPlot) getPlot()).setHighlightSelection(getChartSettings().isShowFog());

		super.settingsChanged();
	}

	@Override
	public void setSelection(BitSet bitSet) {
		if (bitSet != null) {
			((ParallelCoordPlot) getPlot()).setSelectionBitSet(bitSet);
		}
	}

	@Override
	public BitSet getActiveSelection() {
		return ((ParallelCoordPlot) getPlot()).getSelectionBitSet();
	}

	@Override
	public BitSet getSelection(Shape shape, boolean isSingleSelection) {
		return ((ParallelCoordPlot) getPlot()).getContainedPoints(shape, isSingleSelection);
	}

	@Override
	public MouseAdapter getDragListener(BaseChartView<ENTITY, ITEM> chartView) {
		return new PercentageDragListener(chartView);
	}

	@Override
	public boolean isSupportSVG() {
		return false;
	}

	private class PercentageDragListener extends MouseAdapter implements MouseMotionListener {
		private java.awt.Point destination;
		private java.awt.Point origin;
		private double[] multipliers;
		private double[][] bounds;
		private BaseChartView<ENTITY, ITEM> chartView;

		public PercentageDragListener(BaseChartView<ENTITY, ITEM> chartView) {
			this.chartView = chartView;
		}

		@Override
		public void mousePressed(MouseEvent evt) {
			origin = evt.getLocationOnScreen();
			bounds = getDataProvider().getDataBounds();
			multipliers = new double[bounds.length];
			int index = 0;
			for (double[] bound : bounds) {
				multipliers[index++] = (bound[0] - bound[1]) / getChartComponent().getHeight();
			}
		}

		@Override
		public void mouseDragged(MouseEvent evt) {
			destination = evt.getLocationOnScreen();
			double diff = destination.y - origin.y;
			for (int i = 0; i < bounds.length; i++) {
				bounds[i][0] = bounds[i][0] - diff * multipliers[i];
				bounds[i][1] = bounds[i][1] - diff * multipliers[i];
			}

			origin = destination;
			chartView.setDataBoundsForAllLayers(bounds);
		}
	}

}
