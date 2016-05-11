package eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.CELL_IMAGE;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType.TWO_DIMENSIONAL;

import java.awt.event.ComponentEvent;
import java.util.BitSet;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.TablePlot;
import uk.ac.starlink.ttools.plot.TooltipsPlot;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public abstract class TooltipsChart<ENTITY, ITEM> extends AbstractChart<ENTITY, ITEM> {

	private static final String _RESIZE = "_RESIZE";

	private ITooltipProvider tooltipProvider;

	public TooltipsChart() {
		setType(TWO_DIMENSIONAL);
		setName(CELL_IMAGE);
	}

	@Override
	public TablePlot createPlot() {
		PtPlotSurface plotSurface = new PtPlotSurface();
		plotSurface.setPreserveAxesSpace(isPreserveAxesSpace());
		TooltipsPlot plot = new TooltipsPlot(plotSurface);
		return plot;
	}

	@Override
	public void setSelection(BitSet bitSet) {
		// Generate the tooltips based on the bitset.
		ITooltipProvider tooltipProvider = getTooltipProvider();
		tooltipProvider.setConfig(getChartSettings().getTooltipSettings());
		((TooltipsPlot) getPlot()).setTooltipProvider(tooltipProvider);
		JobUtils.runUserJob(monitor -> {
			for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
				if (monitor.isCanceled()) return;
				tooltipProvider.getTooltipSize(i);
				monitor.worked(1);
			}
			((TooltipsPlot) getPlot()).setSelection(bitSet);
			repaint();
		}, "Loading Tooltips", bitSet.cardinality(), this.toString(), null);
		super.setSelection(bitSet);
	}

	@Override
	public void settingsChanged() {
		((TooltipsPlot) getPlot()).setFontSize(getChartSettings().getTooltipSettings().getFontSize());
		((TooltipsPlot) getPlot()).setShowLabels(getChartSettings().getTooltipSettings().isShowLabels());
		((TooltipsPlot) getPlot()).setShowCoords(getChartSettings().getTooltipSettings().isShowCoords());
		// Set additional tooltip settings.
		ITooltipProvider tooltipProvider = ((TooltipsPlot) getPlot()).getTooltipProvider();
		if (tooltipProvider != null) {
			tooltipProvider.setConfig(getChartSettings().getTooltipSettings());
		}
		super.settingsChanged();
	}

	@Override
	public void updatePlotData() {
		if (getDataProvider() == null) {
			return;
		}

		// New input should update the TooltipProvider.
		updateTooltipProvider();

		// Set per-axis characteristics.
		int axesCount = getType().getNumberOfDimensions();
		ColumnInfo[] axinfos = new ColumnInfo[axesCount];
		ValueConverter[] converters = new ValueConverter[axesCount];
		for (int i = 0; i < axesCount; i++) {
			int index = getDataProvider().getFeatureIndex(getDataProvider().getSelectedFeature(i));
			ColumnInfo cinfo = getDataProvider().getColumnInfo(index);
			axinfos[i] = cinfo;
			converters[i] = (ValueConverter) cinfo.getAuxDatumValue(TopcatUtils.NUMERIC_CONVERTER_INFO,
					ValueConverter.class);
		}
		PlotState state = new PlotState();
		setPlotState(state);

		state.setPlotData(createPlotData());
		state.setMainNdim(getType().getNumberOfDimensions());
		state.setGrid(true);
		state.setAntialias(false);

		state.setAxes(axinfos);
		state.setConverters(converters);
		state.setLogFlags(new boolean[getType().getNumberOfDimensions()]);
		state.setFlipFlags(new boolean[getType().getNumberOfDimensions()]);
		state.setShowAxes(false);
		state.setAxisLabels(getDataProvider().getAxisLabels());

		state.setRanges(getRangeBounds());
		state.setValid(true);
		getPlot().setState(state);
	}

	@Override
	public boolean isSupportSVG() {
		return true;
	}

	@Override
	public void componentResized(ComponentEvent event) {
		// Do not waste precious CPU time while resizing.
		if (getPlotState() != null) getPlotState().setValid(false);
		JobUtils.runBackgroundJob(monitor -> {
			if (getPlotState() != null) getPlotState().setValid(true);
			repaint();
		}, this.toString() + _RESIZE, null, 600);
		super.componentResized(event);
	}

	protected abstract ITooltipProvider createTooltipProvider();

	private ITooltipProvider getTooltipProvider() {
		if (tooltipProvider == null) tooltipProvider = createTooltipProvider();
		return tooltipProvider;
	}

	private void updateTooltipProvider() {
		if (tooltipProvider != null) tooltipProvider.dispose();
		tooltipProvider = createTooltipProvider();
		tooltipProvider.setConfig(getChartSettings().getTooltipSettings());
	}

}