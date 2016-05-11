package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.config;

import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.PEARSONS;
import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.PEARSONS_STANDARD_ERRORS;
import static eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType.SPEARMANS;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;

public class HeatmapConfig implements Serializable {

	private static final long serialVersionUID = 6867642101365292665L;

	public final static String[] VALUE_TYPES = {
		PEARSONS.getName(),
		PEARSONS_STANDARD_ERRORS.getName(),
		SPEARMANS.getName()
	};

	public final static String SETTING_LABEL = "CORRELATION_HEATMAP";
	public final static String SETTING_MIN_RGB = "SETTING_MIN_RGB";
	public final static String SETTING_MEAN_RGB = "SETTING_MEAN_RGB";
	public final static String SETTING_MAX_RGB = "SETTING_MAX_RGB";

	public int valueType;
	public Map<String, String> colorSettings;

	public void loadDefaults(String layerId) {
		String label = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_LABEL);
		String minRgb = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_MIN_RGB);
		String meanRgb = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_MEAN_RGB);
		String maxRgb = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, SETTING_MEAN_RGB);

		// Default is the same as for Value Layer
		Activator.getDefault().getPreferenceStore().putValue(Prefs.DEFAULT_CORRELATION_HEATMAP_MIN, "150,50,50");
		Activator.getDefault().getPreferenceStore().putValue(Prefs.DEFAULT_CORRELATION_HEATMAP_MEAN, "255,255,255");
		Activator.getDefault().getPreferenceStore().putValue(Prefs.DEFAULT_CORRELATION_HEATMAP_MAX, "50,50,150");
		if (label == null || label.isEmpty()) label = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_CORRELATION_HEATMAP_LABEL_1);
		if (minRgb == null || minRgb.isEmpty()) minRgb = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_CORRELATION_HEATMAP_MIN);
		if (meanRgb == null || meanRgb.isEmpty()) meanRgb = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_CORRELATION_HEATMAP_MEAN);
		if (maxRgb == null || maxRgb.isEmpty()) maxRgb = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_CORRELATION_HEATMAP_MAX);

		this.valueType = CollectionUtils.find(VALUE_TYPES, label);
		this.colorSettings = new HashMap<>();
		colorSettings.put("min.RGB", minRgb);
		colorSettings.put("mean.RGB", meanRgb);
		colorSettings.put("max.RGB", maxRgb);
	}

	public int getValueType() {
		return valueType;
	}

	public void setValueType(int type, String layerId) {
		valueType = type;
		GridState.saveValue(GridState.ALL_PROTOCOLS, layerId, SETTING_LABEL, VALUE_TYPES[type]);
	}

	public Map<String, String> getColorSettings() {
		return colorSettings;
	}

	public void setColorMethod(HashMap<String, String> settings) {
		this.colorSettings = settings;
	}

}