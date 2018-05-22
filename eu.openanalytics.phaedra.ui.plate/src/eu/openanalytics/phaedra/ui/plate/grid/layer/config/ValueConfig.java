package eu.openanalytics.phaedra.ui.plate.grid.layer.config;

import java.io.Serializable;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.grid.layer.ValueProvider.ValueKey;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;

public class ValueConfig implements Serializable {

	private static final long serialVersionUID = 2283033463959625060L;
	
	public final static String SETTING_LABEL_1 = "LABEL_1";
	public final static String SETTING_LABEL_2 = "LABEL_2";
	public final static String SETTING_LABEL_3 = "LABEL_3";
	public final static String SETTING_FONT_COLOR = "FONT_COLOR";

	public final static int FONT_COLOR_AUTO = 1;
	public final static int FONT_COLOR_BLACK = 2;
	public final static int FONT_COLOR_WHITE = 3;

	private String[] valueKeyIds = new String[] { "", "", "" };
	private int fontColor;

	// Note: this is just a transient cache, for performance.
	private transient ValueKey[] valueKeys = new ValueKey[3];
	
	public void loadDefaults(String layerId) {
		valueKeys = new ValueKey[3];
		valueKeyIds = new String[3];
		valueKeyIds[0] = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_LABEL_1);
		valueKeyIds[1] = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_LABEL_2);
		valueKeyIds[2] = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_LABEL_3);
		String fontColor = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_FONT_COLOR);

		if (valueKeyIds[0] == null || valueKeyIds[0].isEmpty()) valueKeyIds[0] = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_HEATMAP_LABEL_1);
		if (valueKeyIds[1] == null || valueKeyIds[1].isEmpty()) valueKeyIds[1] = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_HEATMAP_LABEL_2);
		if (valueKeyIds[2] == null || valueKeyIds[2].isEmpty()) valueKeyIds[2] = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_HEATMAP_LABEL_3);
		if (fontColor == null || fontColor.isEmpty()) fontColor = Activator.getDefault().getPreferenceStore().getString(Prefs.HEATMAP_FONT_COLOR);
		
		this.fontColor = Integer.parseInt(fontColor);
	}

	public int getValueKeyLength() {
		return valueKeyIds.length;
	}

	public ValueKey getValueKey(int index) {
		if (valueKeys == null) valueKeys = new ValueKey[3];
		if (valueKeys[index] == null) valueKeys[index] = ValueKey.fromIdString(valueKeyIds[index]);
		return valueKeys[index];
	}
	
	public void setValueKey(int index, ValueKey key, String layerId) {
		if (valueKeys == null) valueKeys = new ValueKey[3];
		valueKeys[index] = key;
		valueKeyIds[index] = key.toIdString();
		
		String[] keys = new String[]{ SETTING_LABEL_1, SETTING_LABEL_2, SETTING_LABEL_3 };
		GridState.saveValue(GridState.ALL_PROTOCOLS, layerId, keys[index], valueKeyIds[index]);
	}

	public int getFontColor() {
		return fontColor;
	}

	public void setFontColor(int fontColor, String layerId) {
		this.fontColor = fontColor;
		GridState.saveValue(GridState.ALL_PROTOCOLS, layerId, SETTING_FONT_COLOR, ""+fontColor);
	}
}