package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatterdensity;

import java.util.Arrays;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter3DChart;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.TablePlot;

public class ScatterDensity3DChart<ENTITY, ITEM> extends Scatter3DChart<ENTITY, ITEM> {

	public static final String UNIT_WEIGHT = "Unit Weight";

	private List<String> auxFeatures = Arrays.asList(UNIT_WEIGHT);

	public ScatterDensity3DChart() {
		setName(ChartName.SCATTER_DENSITY_3D);
	}

	@Override
	public TablePlot createPlot() {
		return new ScatterDensity3DPlot();
	}

	@Override
	public void updatePlotData() {
		if (getDataProvider() == null) {
			return;
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

		ScatterDensity3DPlotState state = new ScatterDensity3DPlotState();
		setPlotState(state);
		state.setPlotData(createPlotData());

		state.setMainNdim(getType().getNumberOfDimensions());
		state.setGrid(true);
		state.setRgb(false);
		state.setWeighted(false);
		state.setAntialias(false);

		state.setAxes(axinfos);
		state.setConverters(converters);
		state.setLogFlags(new boolean[getDataProvider().getDimensionCount()]);
		state.setFlipFlags(new boolean[getDataProvider().getDimensionCount()]);
		state.setShowAxes(true);

		/*
		 * Configure the state with this window's current viewing angles and zoom state.
		 */
		state.setRotation(getRotation());
		state.setRotating( false );
		state.setZoomScale( getZoom() );

		/* Configure rendering options. */
		state.setFogginess( getChartSettings().isShowFog() ? 2.0 : 0.0 );
		state.setAxisLabels(getDataProvider().getAxisLabels());
		state.setRanges(getRangeBounds());
		state.setValid(true);
		getPlot().setState(state);
	}

	@Override
	public void settingsChanged() {
		ScatterDensity3DPlotState state = (ScatterDensity3DPlotState) getPlotState();
		AuxiliaryChartSettings settings = getChartSettings().getAuxiliaryChartSettings().get(0);
		state.setLoCut(settings.getLoCut());
		state.setHiCut(settings.getHiCut());
		state.setPixelSize(settings.getPixelSize());
		state.setIndexedShader(settings.getShader());
		state.setShaders(new Shader[] { settings.getShader() });
		state.setValid(true);
		state.setSkipZeroDensity(settings.isSkipZeroDensity());
		super.settingsChanged();
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
