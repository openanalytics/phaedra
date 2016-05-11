package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.FeaturePlateLayer;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.config.HeatmapConfig;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.config.ValueConfig;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.config.ValueConfigDialog;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;

public class ValueLayer extends FeaturePlateLayer {

	private HeatmapLayer heatmapLayer;
	private ValueConfig config;

	@Override
	public String getName() {
		return "Value Labels";
	}

	@Override
	protected void doInitialize() {
		if (config == null) {
			config = new ValueConfig();
			config.loadDefaults(getId());
		}

		// Check if a heatmap layer is present, to adjust text color.
		for (IGridLayer layer : getLayerSupport().getLayers()) {
			if (layer instanceof HeatmapLayer) heatmapLayer = ((HeatmapLayer) layer);
		}
	}

	@Override
	public IGridCellRenderer createRenderer() {
		return new LabelRenderer();
	}

	@Override
	public void update(GridCell cell, Object modelObject) {
		if (!hasSelection()) return;
	}

	@Override
	public boolean hasConfigDialog() {
		return true;
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return new ValueConfigDialog(shell, this);
	}

	private class LabelRenderer extends BaseGridCellRenderer {

		private Font font;
		private IPropertyChangeListener prefListener;
		private Map<String, Font> fontCache;

		public LabelRenderer() {
			fontCache = new HashMap<>();
			FontData[] fontData = null;
			Boolean isHidden = (Boolean) getLayerSupport().getAttribute(GridLayerSupport.IS_HIDDEN);
			if (isHidden == null || !isHidden) {
				fontData = PreferenceConverter.getFontDataArray(Activator.getDefault().getPreferenceStore(), Prefs.HEATMAP_FONT);
			} else {
				fontData = new FontData[] { new FontData("Calibri", 7, SWT.NONE) };
			}
			setFont(fontData);

			prefListener = new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(Prefs.HEATMAP_FONT)) {
						FontData[] newFont = null;
						if (event.getNewValue() instanceof FontData[]) {
							newFont = (FontData[]) event.getNewValue();
						}
						setFont(newFont);
						getLayerSupport().getViewer().getGrid().redraw();

					}
				}
			};
			Activator.getDefault().getPreferenceStore().addPropertyChangeListener(prefListener);
		}

		private void setFont(FontData[] data) {
			if (data != null) {
				String key = data[0].getName() + "#" + data[0].getHeight() + "#" + data[0].getStyle();
				if (!fontCache.containsKey(key)) {
					fontCache.put(key, new Font(null, data));
				}
				font = fontCache.get(key);
			} else {
				font = null;
			}
		}

		@Override
		public void dispose() {
			Activator.getDefault().getPreferenceStore().removePropertyChangeListener(prefListener);
			for (Font f: fontCache.values()) f.dispose();
			if (font != null) font.dispose();
			super.dispose();
		}

		@Override
		public Image getImage(GridCell cell) {
			return null;
		}

		@Override
		public Color getBgColor(GridCell cell) {
			return null;
		}

		@Override
		public Font getFont(GridCell cell) {
			return font;
		}

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasSelection() || cell.getData() == null) return;

			boolean heatmapEnabled = (heatmapLayer != null && heatmapLayer.isEnabled());

			RGB rgb = null;

			Color color = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
			if (config.getFontColor() == ValueConfig.FONT_COLOR_BLACK) {
				// Keep the color black.
			} else if (config.getFontColor() == ValueConfig.FONT_COLOR_WHITE) {
				color = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
			} else if (heatmapEnabled) {
				int valueType = ((HeatmapConfig)heatmapLayer.getConfig()).getValueType();
				double value = HeatmapProvider.getValue(valueType, getMatrix(), cell.getRow(), cell.getColumn());
				rgb = heatmapLayer.getColorMethod().getColor(value);
				color = ColorUtils.getTextColor(rgb);
			}
			gc.setForeground(color);

			super.render(cell, gc, x, y, w, h);
		}

		@SuppressWarnings("unchecked")
		@Override
		public String getTooltipContribution(GridCell cell) {
			Object o = cell.getData();
			if (o == null) return "";
			List<Feature> features = (List<Feature>) o;

			StringBuilder tooltipText = new StringBuilder();
			for (int i = 1; i < ValueConfig.VALUE_TYPES.length; i++) {
				if (tooltipText.length() > 0) {
					tooltipText.append("\n");
				}
				tooltipText.append(ValueConfig.VALUE_TYPES[i]);
				tooltipText.append(": ");
				tooltipText.append(ValueProvider.getValue(i, getMatrix(), cell.getColumn(), cell.getRow(), features));
			}

			return tooltipText.toString();
		}

		@SuppressWarnings("unchecked")
		@Override
		public String[] getLabels(GridCell cell) {
			Object o = cell.getData();
			if (o == null) return new String[0];
			List<Feature> features = (List<Feature>) o;

			String[] labels = new String[config.getValueTypeLength()];
			for (int i = 0; i < labels.length; i++) {
				labels[i] = ValueProvider.getValue(config.getValueType(i), getMatrix(), cell.getColumn(), cell.getRow(), features);
			}

			return labels;
		}

	}

	@Override
	public void setConfig(Object config) {
		this.config = (ValueConfig) config;
	}

	@Override
	public Object getConfig() {
		return config;
	}

}