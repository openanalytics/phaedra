package eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour;


import java.awt.Shape;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.DensityStyle;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.TablePlot;

public class Contour2DChart<ENTITY, ITEM> extends AbstractChart<ENTITY, ITEM> {

	public static final String UNIT_WEIGHT = "Unit Weight";

	private List<String> auxFeatures = Arrays.asList(UNIT_WEIGHT);

	public Contour2DChart() {
		setType(ChartType.TWO_DIMENSIONAL);
		setName(ChartName.CONTOUR_2D);
	}

	@Override
	public TablePlot createPlot() {
		PtPlotSurface plotSurface = new PtPlotSurface();
		plotSurface.setPreserveAxesSpace(isPreserveAxesSpace());
		return new ContourPlot(plotSurface);
	}

	@Override
	public void updatePlotData() {
		if (getDataProvider() == null) {
			return;
		}
		if (!ContourPlotSettings.hasValidSettings(getChartSettings())) {
			ContourPlotSettings.loadDefaults(getChartSettings());
		}

		PtPlotSurface surface = (PtPlotSurface)((ContourPlot)getPlot()).getSurface();
		applyAxisValueLabels(surface);
		surface.setTitle(getDataProvider().getTitle());

		// add aux feature per default if nothing set
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

		ContourPlotState state = new ContourPlotState();
		setPlotState(state);
		state.setPlotData(createPlotData());
		state.setMainNdim(getType().getNumberOfDimensions());
		state.setRgb(false);
		state.setWeighted(false);
		state.setAntialias(false);
		state.setAxes(axinfos);
		state.setConverters(converters);
		state.setLogFlags(new boolean[getDataProvider().getDimensionCount()]);
		state.setFlipFlags(new boolean[getDataProvider().getDimensionCount()]);
		state.setShowAxes(false);
		state.setAxisLabels(getDataProvider().getAxisLabels());
		state.setRanges(getRangeBounds());
		state.setValid(true);
		getPlot().setState(state);
	}

	@Override
	public Style[] getStyles() {
		DensityStyle style = new DensityStyle(DensityStyle.GREEN) {
			@Override
			protected boolean isRGB() {
				return false;
			}
		};
		AuxiliaryChartSettings settings = getChartSettings().getAuxiliaryChartSettings().get(0);
		style.setShader(settings.getShader());
		style.setOpacity(Math.abs(settings.getTransparancy() - 1));
		return new Style[] { style };
	}

	@Override
	public void settingsChanged() {
		// Transfer settings (e.g. from settings dialog) to the current plot state.
		ContourPlotState state = (ContourPlotState) getPlotState();
		ContourPlotSettings.transferToState(getChartSettings(), state);
		state.setValid(true);
		super.settingsChanged();
	}

	@Override
	public void setSelection(BitSet bitSet) {
		if (bitSet != null) {
			((ContourPlot) getPlot()).setSelectionBitSet(bitSet);
		}
	}

	@Override
	public BitSet getActiveSelection() {
		return ((ContourPlot) getPlot()).getSelectionBitSet();
	}

	@Override
	public BitSet getSelection(Shape shape, boolean isSingleSelection) {
//		if (isSingleSelection) {
//			BitSet selection = new BitSet();
//			Rectangle bounds = shape.getBounds();
//			Point point = new Point(bounds.x, bounds.y);
//			int index = ((ContourPlot) getPlot()).getPlottedPointIterator().getClosestPoint(point, 10);
//			if (index >= 0) {
//				selection.set(index);
//			}
//			return selection;
//		}
		//return ((ContourPlot) getPlot()).getPlottedPointIterator().getContainedPoints(shape);
		return ((ContourPlot) getPlot()).getSelectedBitSet(shape);
	}

	@Override
	public boolean isStackable() {
		return true;
	}

	@Override
	public boolean isSupportSVG() {
		return false;
	}
}
