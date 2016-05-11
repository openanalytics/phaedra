package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.config;

import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.PEARSONS;
import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.PEARSONS_STANDARD_ERRORS;
import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.SPEARMANS;

import java.io.Serializable;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;

public class ValueConfig implements Serializable {

	private static final long serialVersionUID = 5147053122714235061L;

	public final static String[] VALUE_TYPES = {
		"No Label",
		"Features",
		PEARSONS.getName(),
		PEARSONS_STANDARD_ERRORS.getName(),
		SPEARMANS.getName()
	};

	public final static String SETTING_LABEL_1 = "CORRELATION_LABEL_1";
	public final static String SETTING_LABEL_2 = "CORRELATION_LABEL_2";
	public final static String SETTING_LABEL_3 = "CORRELATION_LABEL_3";
	public final static String SETTING_FONT_COLOR = "CORRELATION_FONT_COLOR";

	public final static int FONT_COLOR_AUTO = 1;
	public final static int FONT_COLOR_BLACK = 2;
	public final static int FONT_COLOR_WHITE = 3;

	public int[] valueTypes;
	public int fontColor;

	public void loadDefaults(String layerId) {
		String[] labels = new String[3];
		valueTypes = new int[3];

		labels[0] = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_LABEL_1);
		labels[1] = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_LABEL_2);
		labels[2] = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_LABEL_3);
		String fontColor = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_FONT_COLOR);

		 Activator.getDefault().getPreferenceStore().putValue(Prefs.HEATMAP_CORRELATION_FONT_COLOR, ValueConfig.FONT_COLOR_AUTO + "");
		if (labels[0] == null || labels[0].isEmpty()) labels[0] = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_CORRELATION_HEATMAP_LABEL_1);
		if (labels[1] == null || labels[1].isEmpty()) labels[1] = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_CORRELATION_HEATMAP_LABEL_2);
		if (labels[2] == null || labels[2].isEmpty()) labels[2] = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_CORRELATION_HEATMAP_LABEL_3);
		if (fontColor == null || fontColor.isEmpty()) fontColor = Activator.getDefault().getPreferenceStore().getString(Prefs.HEATMAP_CORRELATION_FONT_COLOR);

		valueTypes[0] = CollectionUtils.find(VALUE_TYPES, labels[0]);
		valueTypes[1] = CollectionUtils.find(VALUE_TYPES, labels[1]);
		valueTypes[2] = CollectionUtils.find(VALUE_TYPES, labels[2]);
		this.fontColor = Integer.parseInt(fontColor);
	}

	public int getValueTypeLength() {
		return valueTypes.length;
	}

	public int getValueType(int index) {
		return valueTypes[index];
	}

	public void setValueType(int index, int type, String layerId) {
		valueTypes[index] = type;

		String[] labels = new String[]{SETTING_LABEL_1, SETTING_LABEL_2, SETTING_LABEL_3};
		GridState.saveValue(GridState.ALL_PROTOCOLS, layerId, labels[index], VALUE_TYPES[type]);
	}

	public int getFontColor() {
		return fontColor;
	}

	public void setFontColor(int fontColor, String layerId) {
		this.fontColor = fontColor;
		GridState.saveValue(GridState.ALL_PROTOCOLS, layerId, SETTING_FONT_COLOR, ""+fontColor);
	}

}