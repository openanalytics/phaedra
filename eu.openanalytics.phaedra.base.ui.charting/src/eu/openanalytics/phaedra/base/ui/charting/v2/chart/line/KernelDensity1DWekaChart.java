package eu.openanalytics.phaedra.base.ui.charting.v2.chart.line;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.DENSITY_1D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartType.ONE_DIMENSIONAL;

import java.awt.Shape;
import java.util.BitSet;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataCalculator;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.KernelDensityWekaDataCalculator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.SurfacePlot;

public class KernelDensity1DWekaChart<ENTITY, ITEM> extends Line2DChart<ENTITY, ITEM> {

	private static final String DENSITY = "Density";

	public KernelDensity1DWekaChart() {
		setType(ONE_DIMENSIONAL);
		setName(DENSITY_1D);
	}

	@Override
	public void updatePlotData() {
		if (getDataProvider() == null) {
			return;
		}
		getDataProvider().setDataCalculator(getDataCalculator());

		PtPlotSurface surface = (PtPlotSurface)((SurfacePlot)getPlot()).getSurface();
		applyAxisValueLabels(surface);
		surface.setTitle(getDataProvider().getTitle());

		/* Set per-axis characteristics. */
		ColumnInfo[] axinfos = new ColumnInfo[] { getDataProvider().getColumnInfo(0),
				new ColumnInfo(new DefaultValueInfo(DENSITY, Float.class, DENSITY)) };

		ValueConverter[] converters = new ValueConverter[] {
				(ValueConverter) axinfos[0].getAuxDatumValue(TopcatUtils.NUMERIC_CONVERTER_INFO, ValueConverter.class),
				(ValueConverter) axinfos[1].getAuxDatumValue(TopcatUtils.NUMERIC_CONVERTER_INFO, ValueConverter.class) };

		PlotState state = new PlotState();
		setPlotState(state);
		state.setPlotData(createPlotData());

		state.setMainNdim(2);
		state.setAntialias(true);

		state.setAxes(axinfos);
		state.setConverters(converters);
		state.setLogFlags(new boolean[2]); // even if one dimension, 2 axes can

		state.setFlipFlags(new boolean[2]);
		state.setShowAxes(false);
		state.setAxisLabels(new String[] { getDataProvider().getSelectedFeature(0), DENSITY });
		state.setRanges(getRangeBounds());

		state.setValid(true);
		getPlot().setState(state);
	}

	@Override
	public IDataCalculator<ENTITY, ITEM> getDataCalculator() {
		return new KernelDensityWekaDataCalculator<ENTITY, ITEM>(getDataProvider(), getChartSettings());
	}

	@Override
	public void setSelection(BitSet bitSet) {
		super.setSelection(getDataProvider().getDataCalculator().calculateSelection(bitSet));
	}

	@Override
	public BitSet getActiveSelection() {
		return getDataProvider().getDataCalculator().deCalculateSelection(super.getActiveSelection());
	}

	@Override
	public BitSet getSelection(Shape shape, boolean isSingleSelection) {
		return getDataProvider().getDataCalculator().deCalculateSelection(super.getSelection(shape, isSingleSelection));
	}
}