package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SCATTER_2D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType.TWO_DIMENSIONAL;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.ScatterPlot;
import uk.ac.starlink.ttools.plot.Shader;

public class Scatter2DChart<ENTITY, ITEM> extends AbstractChart<ENTITY, ITEM> {

	public Scatter2DChart() {
		setType(TWO_DIMENSIONAL);
		setName(SCATTER_2D);
	}

	@Override
	public ScatterPlot createPlot() {
		PtPlotSurface plotSurface = new PtPlotSurface();
		plotSurface.setAxes(false);
		plotSurface.setPreserveAxesSpace(isPreserveAxesSpace());
		return new ScatterPlot(plotSurface);
	}

	@Override
	public void updatePlotData() {
		if (getDataProvider() == null) {
			return;
		}

		PtPlotSurface surface = (PtPlotSurface)((ScatterPlot)getPlot()).getSurface();
		applyAxisValueLabels(surface);
		surface.setTitle(getDataProvider().getTitle());

		/* Set per-axis characteristics. */
		int axesCount = getType().getNumberOfDimensions();
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
	public void settingsChanged() {
		((ScatterPlot) getPlot()).setOpacity(getChartSettings().getOpacity());

		// handle aux settings
		if (getChartSettings().getAuxiliaryChartSettings() != null) {
			List<Shader> shaders = new ArrayList<Shader>();
			for (AuxiliaryChartSettings auxSettings : getChartSettings().getAuxiliaryChartSettings()) {
				if (auxSettings.getWeightFeature() != null && !"".equals(auxSettings.getWeightFeature())) {
					shaders.add(auxSettings.getShader());
				}
			}
			if (getPlotState() != null) {
				getPlotState().setShaders(shaders.toArray(new Shader[shaders.size()]));
			}
		}

		super.settingsChanged();
	}

	@Override
	public void setSelection(BitSet bitSet) {
		if (bitSet != null) {
			((ScatterPlot) getPlot()).setSelectionBitSet(bitSet);
		}
	}

	@Override
	public BitSet getActiveSelection() {
		return ((ScatterPlot) getPlot()).getSelectionBitSet();
	}

	@Override
	public BitSet getSelection(Shape shape, boolean isSingleSelection) {
		if (isSingleSelection) {
			BitSet selection = new BitSet();
			Rectangle bounds = shape.getBounds();
			Point point = new Point(bounds.x, bounds.y);
			int index = ((ScatterPlot) getPlot()).getPlottedPointIterator().getClosestPoint(point, 10);
			if (index >= 0) {
				selection.set(index);
			}
			return selection;
		}
		return ((ScatterPlot) getPlot()).getPlottedPointIterator().getContainedPoints(shape);
	}

	@Override
	public boolean isStackable() {
		return true;
	}

	@Override
	public boolean isSupportSVG() {
		return true;
	}
}