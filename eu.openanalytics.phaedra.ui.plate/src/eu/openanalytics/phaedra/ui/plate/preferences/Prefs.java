package eu.openanalytics.phaedra.ui.plate.preferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.BaseGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.grid.layer.ValueProvider;
import eu.openanalytics.phaedra.ui.plate.grid.layer.ValueProvider.ValueKey;
import eu.openanalytics.phaedra.ui.plate.grid.layer.config.ValueConfig;

public class Prefs extends AbstractPreferenceInitializer {

	public final static String DEFAULT_HEATMAP_LABEL_1 = "DEFAULT_HEATMAP_VALUE_LABEL_1";
	public final static String DEFAULT_HEATMAP_LABEL_2 = "DEFAULT_HEATMAP_VALUE_LABEL_2";
	public final static String DEFAULT_HEATMAP_LABEL_3 = "DEFAULT_HEATMAP_VALUE_LABEL_3";

	public final static String DEFAULT_CORRELATION_HEATMAP_LABEL_1 = "DEFAULT_CORRELATION_HEATMAP_LABEL_1";
	public final static String DEFAULT_CORRELATION_HEATMAP_LABEL_2 = "DEFAULT_CORRELATION_HEATMAP_LABEL_2";
	public final static String DEFAULT_CORRELATION_HEATMAP_LABEL_3 = "DEFAULT_CORRELATION_HEATMAP_LABEL_3";

	public final static String DEFAULT_CORRELATION_HEATMAP_MIN = "DEFAULT_CORRELATION_HEATMAP_MIN";
	public final static String DEFAULT_CORRELATION_HEATMAP_MEAN = "DEFAULT_CORRELATION_HEATMAP_MEAN";
	public final static String DEFAULT_CORRELATION_HEATMAP_MAX = "DEFAULT_CORRELATION_HEATMAP_MAX";

	public final static String HEATMAP_FONT = "HEATMAP_FONT";
	public final static String HEATMAP_FONT_COLOR = "HEATMAP_FONT_COLOR";
	public final static String HEATMAP_CORRELATION_FONT_COLOR = "HEATMAP_CORRELATION_FONT_COLOR";

	public final static String HEATMAP_ANNOTATIONS = "HEATMAP_ANNOTATIONS";
	public final static String HEATMAP_ANNOTATION_WELL_COLOR = "HEATMAP_ANNOTATION_WELL_COLOR";
	public final static String HEATMAP_ANNOTATION_SUBWELL_COLOR = "HEATMAP_ANNOTATION_SUBWELL_COLOR";
	public final static String HEATMAP_ANNOTATION_SIZE = "HEATMAP_ANNOTATION_SIZE";

	public final static String WELL_TABLE_COLORS = "WELL_TABLE_COLORS";

	public final static String SHOW_DEFAULT = "SHOW_DEFAULT_";

	public final static String SVG_NO_BG = "SVG_NO_BG";
	public final static String SVG_BG_COLOR = "SVG_BG_COLOR";

	public final static String MULTI_FEATURE_MIN_SIZE = "MULTI_FEATURE_MIN_SIZE";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(DEFAULT_HEATMAP_LABEL_1, ValueKey.create(ValueProvider.VALUE_TYPE_ACTIVE_FEATURE).toIdString());
		store.setDefault(DEFAULT_HEATMAP_LABEL_2, ValueKey.create(ValueProvider.VALUE_TYPE_PROPERTY, WellProperty.Compound).toIdString());
		store.setDefault(DEFAULT_HEATMAP_LABEL_3, ValueKey.create(ValueProvider.VALUE_TYPE_NONE).toIdString());
		store.setDefault(DEFAULT_CORRELATION_HEATMAP_LABEL_1, "Pearsons Correlation");
		store.setDefault(DEFAULT_CORRELATION_HEATMAP_LABEL_2, "No Label");
		store.setDefault(DEFAULT_CORRELATION_HEATMAP_LABEL_3, "No Label");
		store.setDefault(DEFAULT_CORRELATION_HEATMAP_MIN, "150,50,50");
		store.setDefault(DEFAULT_CORRELATION_HEATMAP_MEAN, "255,255,255");
		store.setDefault(DEFAULT_CORRELATION_HEATMAP_MAX, "50,50,150");
		store.setDefault(HEATMAP_FONT, "");
		store.setDefault(HEATMAP_FONT_COLOR, ValueConfig.FONT_COLOR_AUTO);
		store.setDefault(HEATMAP_CORRELATION_FONT_COLOR, ValueConfig.FONT_COLOR_AUTO);
		store.setDefault(HEATMAP_ANNOTATIONS, false);
		PreferenceConverter.setDefault(store, HEATMAP_ANNOTATION_SUBWELL_COLOR, new RGB(0,255,0));
		PreferenceConverter.setDefault(store, HEATMAP_ANNOTATION_WELL_COLOR, new RGB(255,0,0));
		store.setDefault(HEATMAP_ANNOTATION_SIZE, 15);
		store.setDefault(WELL_TABLE_COLORS, true);

		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IGridLayer.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IGridLayer.ATTR_CLASS);
				if (o instanceof IGridLayer) {
					IGridLayer layer = (IGridLayer) o;
					if (layer instanceof BaseGridLayer) {
						BaseGridLayer baseLayer = (BaseGridLayer) layer;
						String defaultEnabled = el.getAttribute(IGridLayer.ATTR_DEFAULT_ENABLED);
						if (defaultEnabled == null || defaultEnabled.equalsIgnoreCase("true")) {
							store.setDefault(SHOW_DEFAULT + baseLayer.getClass(), true);
						} else {
							store.setDefault(SHOW_DEFAULT + baseLayer.getClass(), false);
						}
					}
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}

		store.setDefault(SVG_NO_BG, true);
		PreferenceConverter.setDefault(store, SVG_BG_COLOR, new RGB(255,255,255));

		store.setDefault(MULTI_FEATURE_MIN_SIZE, 5);
	}
}
