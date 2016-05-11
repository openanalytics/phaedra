package eu.openanalytics.phaedra.base.ui.charting.v2.chart.histogram;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.HISTOGRAM_1D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType.ONE_DIMENSIONAL;

import java.awt.Shape;
import java.util.BitSet;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.BinnedDataCalculator;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataCalculator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.Histogram;
import uk.ac.starlink.ttools.plot.HistogramPlotState;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.TablePlot;

public class Histogram1DChart<ENTITY, ITEM> extends AbstractChart<ENTITY, ITEM> {

	public Histogram1DChart() {
		setType(ONE_DIMENSIONAL);
		setName(HISTOGRAM_1D);
	}

	@Override
	public TablePlot createPlot() {
		//render plot as bar chart
		getChartSettings().setBars(true);

		PtPlotSurface plotSurface = new PtPlotSurface();
		plotSurface.setAxes(false);
		plotSurface.setPreserveAxesSpace(isPreserveAxesSpace());
		return new Histogram(plotSurface);
	}

	@Override
	public void updatePlotData() {
		if (getDataProvider() == null) {
			return;
		}

		getDataProvider().setDataCalculator(getDataCalculator());
		Histogram histogram = (Histogram) getPlot();
		PtPlotSurface surface = (PtPlotSurface)histogram.getSurface();
		applyAxisValueLabels(surface);
		surface.setTitle(getDataProvider().getTitle());

		/* Set per-axis characteristics. */
		int axesCount = getType().getNumberOfDimensions();
		ColumnInfo[] axinfos = new ColumnInfo[axesCount];
		ValueConverter[] converters = new ValueConverter[axesCount];
		for (int i = 0; i < axesCount; i++) {
			int index = getDataProvider().getFeatureIndex(getDataProvider().getSelectedFeature(i));
			axinfos[i] = getDataProvider().getColumnInfo(index);
			converters[i] = (ValueConverter) axinfos[i].getAuxDatumValue(TopcatUtils.NUMERIC_CONVERTER_INFO,
					ValueConverter.class);
		}
		HistogramPlotState state = new HistogramPlotState();
		setPlotState(state);
		state.setPlotData(createPlotData());

		state.setMainNdim(getType().getNumberOfDimensions());
		state.setAntialias(false);

		state.setAxes(axinfos);
		state.setConverters(converters);
		state.setLogFlags(new boolean[2]); //even if one dimension, 2 axes can be logaritmic
		state.setFlipFlags(new boolean[2]);
		state.setShowAxes(false);

		/* Set axis labels configured in the axis editor window. */
		state.setAxisLabels(getDataProvider().getAxisLabels());
		state.setBinWidth(getChartSettings().getBinWidth());
		state.setRanges(getRangeBounds());

		state.setValid(true);
		getPlot().setState(state);
	}

	@Override
	public void settingsChanged() {
		((Histogram) getPlot()).setOpacity(getChartSettings().getOpacity());
		HistogramPlotState state = (HistogramPlotState) getPlotState();
		if(state.getBinWidth()!=getChartSettings().getBinWidth()){
			state.setBinWidth(getChartSettings().getBinWidth());
		}
		super.settingsChanged();
	}

	@Override
	public void setSelection(BitSet bitSet) {
		if (bitSet != null) {
			((Histogram) getPlot()).setSelectionBitSet(getDataProvider().getDataCalculator().calculateSelection(bitSet));
		}
	}

	@Override
	public BitSet getActiveSelection() {
		return getDataProvider().getDataCalculator().deCalculateSelection(((Histogram) getPlot()).getSelectionBitSet());
	}

	@Override
	public BitSet getSelection(Shape shape, boolean isSingleSelection) {
		if (isSingleSelection) {
			// Only selects a bar if it's selected near the top of the bar.
			//BitSet selection = new BitSet();
			//Rectangle bounds = shape.getBounds();
			//Point point = new Point(bounds.x, bounds.y);
			//int index = ((Histogram) getPlot()).getPlottedPointIterator().getClosestPoint(point, 10);
			//if (index >= 0) selection.set(index);
			//return getDataProvider().getDataCalculator().deCalculateSelection(selection);
		}
		return getDataProvider().getDataCalculator().deCalculateSelection(((Histogram) getPlot()).getPlottedPointIterator().getContainedPoints(shape));
	}

	@Override
	public boolean isSupportSVG() {
		return false;
	}

	@Override
	public IDataCalculator<ENTITY, ITEM> getDataCalculator() {
		return new BinnedDataCalculator<ENTITY, ITEM>(getDataProvider(), getChartSettings());
	}
}