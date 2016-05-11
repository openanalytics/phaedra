package eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour;

import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;

public class ContourPlotSettings {

	public final static String SETTING_COLOR = "contourColor";
	public final static String SETTING_LEVELS = "contourLevels";
	public final static String SETTING_OFFSET = "contourOffset";
	public final static String SETTING_SMOOTH = "contourSmoothing";
	public final static String SETTING_LEVEL_MODE = "contourLevelMode";

	public static boolean hasValidSettings(ChartSettings settings) {
		return settings.getStringMiscSetting(SETTING_LEVEL_MODE) != null;
	}

	public static void loadDefaults(ChartSettings settings) {
		AuxiliaryChartSettings auxSettings = settings.getAuxiliaryChartSettings().get(0);
		auxSettings.setPixelSize(1);
		settings.setMiscSetting(SETTING_COLOR, new RGB(0,0,0));
		settings.setMiscSetting(SETTING_LEVELS, 5);
		settings.setMiscSetting(SETTING_OFFSET, 0.0);
		settings.setMiscSetting(SETTING_SMOOTH, 30);
		settings.setMiscSetting(SETTING_LEVEL_MODE, LevelMode.LINEAR.toString());
	}

	public static void transferToState(ChartSettings settings, IContourPlotState state) {
		AuxiliaryChartSettings auxSettings = settings.getAuxiliaryChartSettings().get(0);
		state.setLoCut(auxSettings.getLoCut());
		state.setHiCut(auxSettings.getHiCut());
		state.setPixelSize(auxSettings.getPixelSize());
		state.setColor(settings.getRGBMiscSetting(SETTING_COLOR));
		state.setLevels(settings.getIntMiscSetting(SETTING_LEVELS));
		state.setOffset(settings.getDoubleMiscSetting(SETTING_OFFSET));
		state.setSmooth(settings.getIntMiscSetting(SETTING_SMOOTH));
		state.setLevelMode(settings.getStringMiscSetting(SETTING_LEVEL_MODE));
	}
}
