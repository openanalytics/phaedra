package eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.AXES_DYNAMIC;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType.DYNAMIC;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.ParallelCoordPlot;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.TablePlot;

public class AxesDynamicChart<ENTITY, ITEM> extends AbstractChart<ENTITY, ITEM> {

	public AxesDynamicChart() {
		setType(DYNAMIC);
		setName(AXES_DYNAMIC);
	}

	@Override
	public TablePlot createPlot() {
		getChartSettings().setLines(true);
		ParallelCoordPlot plot = new ParallelCoordPlot(false);
		return plot;
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
		state.setShowAxes(true);

		/* Set axis labels configured in the axis editor window. */
		state.setAxisLabels(getDataProvider().getAxisLabels());

		state.setRanges(getRangeBounds());
		state.setValid(true);
		getPlot().setState(state);
	}

	@Override
	public void settingsChanged() {
		getPlotState().setGrid(getChartSettings().isShowGridLines());

		super.settingsChanged();
	}

	@Override
	public boolean isSupportSVG() {
		return true;
	}

}