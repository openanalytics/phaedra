package eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.AXES_2D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType.TWO_DIMENSIONAL;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.AxesDataCalculator;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataCalculator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.ScatterPlot;
import uk.ac.starlink.ttools.plot.SurfacePlot;
import uk.ac.starlink.ttools.plot.TablePlot;

public class Axes2DChart<ENTITY, ITEM> extends AbstractChart<ENTITY, ITEM> {

	public Axes2DChart() {
		setType(TWO_DIMENSIONAL);
		setName(AXES_2D);
	}

	@Override
	public TablePlot createPlot() {
		ScatterPlot plot = new ScatterPlot(new PtPlotSurface());
		plot.setOpaque(false);
		return plot;
	}

	@Override
	public void updatePlotData() {
		if (getDataProvider() == null) {
			return;
		}

		PtPlotSurface surface = (PtPlotSurface)((SurfacePlot)getPlot()).getSurface();
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
		state.setMainNdim(axesCount);
		state.setGrid(getChartSettings().isShowGridLines());
		state.setAntialias(false);

		state.setAxes(axinfos);
		state.setConverters(converters);
		state.setLogFlags(new boolean[axesCount]);
		state.setFlipFlags(new boolean[axesCount]);
		state.setShowAxes(true);
		state.setAxisLabels(getDataProvider().getAxisLabels());

		state.setRanges(getRangeBounds());
		state.setValid(true);
		getPlot().setState(state);
	}

	@Override
	public void settingsChanged() {
		getPlotState().setGrid(getChartSettings().isShowGridLines());
		getPlot().setState(getPlotState());
		super.settingsChanged();
	}

	@Override
	public IDataCalculator<ENTITY, ITEM> getDataCalculator() {
		return new AxesDataCalculator<>(getDataProvider());
	}

	@Override
	public boolean isSupportSVG() {
		return true;
	}

}