package eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour;

import java.awt.Shape;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter3DChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseChartView;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.InteractiveChartView;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.TablePlot;

public class Contour3DChart<ENTITY, ITEM> extends Scatter3DChart<ENTITY, ITEM> {

	public static final String UNIT_WEIGHT = "Unit Weight";

	private List<String> auxFeatures = Arrays.asList(UNIT_WEIGHT);

	public Contour3DChart() {
		setName(ChartName.CONTOUR_3D);
	}

	@Override
	public TablePlot createPlot() {
		return new Contour3DPlot();
	}

	@Override
	public void updatePlotData() {
		if (getDataProvider() == null) {
			return;
		}
		if (!ContourPlotSettings.hasValidSettings(getChartSettings())) {
			ContourPlotSettings.loadDefaults(getChartSettings());
		}

		// Add aux feature per default if nothing set
		if (getDataProvider().getAuxiliaryFeatures() == null || getDataProvider().getAuxiliaryFeatures().isEmpty()) {
			getDataProvider().setAuxiliaryFeatures(auxFeatures);
		}

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

		Contour3DPlotState state = new Contour3DPlotState();
		setPlotState(state);
		state.setPlotData(createPlotData());

		state.setMainNdim(getType().getNumberOfDimensions());
		state.setGrid(true);
		state.setAntialias(true);

		state.setAxes(axinfos);
		state.setConverters(converters);
		state.setLogFlags(new boolean[getDataProvider().getDimensionCount()]);
		state.setFlipFlags(new boolean[getDataProvider().getDimensionCount()]);
		state.setShowAxes(true);

		/* Set axis labels configured in the axis editor window. */
		state.setAxisLabels(getDataProvider().getAxisLabels());

		state.setRanges(getRangeBounds());

		/*
		 * Configure the state with this window's current viewing angles and zoom state.
		 */
		state.setRotation(getRotation());
		state.setRotating( false );
		state.setZoomScale( getZoom() );

		/* Configure rendering options. */
		state.setFogginess( getChartSettings().isShowFog() ? 2.0 : 0.0 );
		state.setAntialias(true);
		state.setValid(true);
		getPlot().setState(state);
	}

	@Override
	public void settingsChanged() {
		// Transfer settings (e.g. from settings dialog) to the current plot state.
		Contour3DPlotState state = (Contour3DPlotState) getPlotState();
		ContourPlotSettings.transferToState(getChartSettings(), state);
		state.setValid(true);
		super.settingsChanged();
	}

	@Override
	public void setSelection(BitSet bitSet) {
		if (bitSet != null) {
			((Contour3DPlot) getPlot()).setSelectionBitSet(bitSet);
		}
	}

	@Override
	public BitSet getActiveSelection() {
		return ((Contour3DPlot) getPlot()).getSelectionBitSet();
	}

	@Override
	public BitSet getSelection(Shape shape, boolean isSingleSelection) {
		return ((Contour3DPlot) getPlot()).getSelectedBitSet(shape);
	}

	@Override
	public void setRotation(double[] rotation) {
		super.setRotation(rotation);
	}

	@Override
	public boolean isStackable() {
		return true;
	}

	@Override
	public boolean isSupportSVG() {
		return false;
	}

	@Override
	protected void stopRotating(BaseChartView<ENTITY, ITEM> chartView) {
		super.stopRotating(chartView);
		if (chartView instanceof InteractiveChartView) {
			// Rotating a 3D contour plot with selected levels will change the points that are selected.
			// Make sure other charts receive this updated selection.
			((InteractiveChartView<?, ?>) chartView).updateSelection();
		}
	}

}
