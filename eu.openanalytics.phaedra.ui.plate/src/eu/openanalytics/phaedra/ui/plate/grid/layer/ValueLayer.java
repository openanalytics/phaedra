package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.plate.grid.layer.ValueProvider.ValueKey;
import eu.openanalytics.phaedra.ui.plate.grid.layer.config.ValueConfig;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;
import eu.openanalytics.phaedra.ui.plate.util.FeatureSelectionTree;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.provider.IFeatureProvider;

public class ValueLayer extends PlatesLayer {

	private Feature currentFeature;
	private String currentNormalization;

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
		if (!hasPlates()) return;

		IFeatureProvider provider = ((IFeatureProvider) getLayerSupport().getAttribute("featureProvider"));
		currentFeature = provider.getCurrentFeature();
		currentNormalization = provider.getCurrentNormalization();
	}

	@Override
	public boolean hasConfigDialog() {
		return true;
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return new ConfigDialog(shell);
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

			prefListener = event -> {
				if (event.getProperty().equals(Prefs.HEATMAP_FONT)) {
					FontData[] newFont = null;
					if (event.getNewValue() instanceof FontData[]) {
						newFont = (FontData[]) event.getNewValue();
					}
					setFont(newFont);
					getLayerSupport().getViewer().getGrid().redraw();

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
			if (!isEnabled() || !hasPlates()) return;

			List<Well> wells = getWells(cell.getData());
			if (wells == null || wells.isEmpty()) return;
			Well well = wells.get(0);
			
			RGB rgb = null;
			boolean heatmapEnabled = (heatmapLayer != null && heatmapLayer.isEnabled());
			boolean numericFeature = (currentFeature != null && currentFeature.isNumeric());

			Color color = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
			if (config.getFontColor() == ValueConfig.FONT_COLOR_BLACK) {
				// Keep the color black.
			} else if (config.getFontColor() == ValueConfig.FONT_COLOR_WHITE) {
				color = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
			} else if (heatmapEnabled && numericFeature && wells != null) {
				double value = CalculationService.getInstance().getAccessor(well.getPlate()).getNumericValue(well, currentFeature, currentNormalization);
				rgb = heatmapLayer.getColorMethod().getColor(value);
				color = ColorUtils.getTextColor(rgb);
			}
			gc.setForeground(color);
			super.render(cell, gc, x, y, w, h);
		}

		@Override
		public String[] getLabels(GridCell cell) {
			if (currentFeature == null) return new String[0];

			List<Well> wells = getWells(cell.getData());
			if (wells == null || wells.isEmpty()) return new String[0];
			Well well = wells.get(0);
			
			String[] labels = new String[config.getValueKeyLength()];
			DataFormatter dataFormatter = getDataFormatter();
			for (int i = 0; i < labels.length; i++) {
				ValueKey key = config.getValueKey(i);
				if (ValueProvider.VALUE_TYPE_ACTIVE_FEATURE.equals(key.valueType)) {
					key.arg1 = currentFeature;
					key.arg2 = currentNormalization;
				}
				labels[i] = ValueProvider.getValue(well, key, dataFormatter);
			}
			return labels;
		}

		@Override
		public String getTooltipContribution(GridCell cell) {
			String[] labels = getLabels(cell);
			if (labels == null) return null;
			return Arrays.stream(labels).filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining("\n"));
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

	private class ConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

		private ValueKey[] selectedValues;
		private Combo fontColorCombo;
		private int selectedFontColor;

		public ConfigDialog(Shell parentShell) {
			super(parentShell);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Configuration: " + getName());
		}

		@Override
		protected Control createDialogArea(Composite parent) {

			Composite area = (Composite) super.createDialogArea(parent);
			Composite container = new Composite(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).applyTo(container);

			selectedValues = new ValueKey[config.getValueKeyLength()];
			for (int i = 0; i < selectedValues.length; i++) {
				final int nr = i;
				selectedValues[i] = config.getValueKey(i);
				
				Label lbl = new Label(container, SWT.NONE);
				lbl.setText("Label " + (i + 1) + ":");

				Label valueLbl = new Label(container, SWT.NONE);
				valueLbl.setText(selectedValues[i].toString());
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(valueLbl);
				
				Button selectFeatureBtn = new Button(container, SWT.PUSH);
				selectFeatureBtn.setText("Select...");
				selectFeatureBtn.addListener(SWT.Selection, e -> {
					ProtocolClass pClass = currentFeature == null ? ProtocolUIService.getInstance().getCurrentProtocolClass() : currentFeature.getProtocolClass();
					FeatureSelectionTree.open(pClass, getShell().getLocation(), sel -> {
						selectedValues[nr] = FeatureSelectionTree.toKey(sel);
						valueLbl.setText(selectedValues[nr].toString());
					});
				});
			}

			Label lbl = new Label(container, SWT.NONE);
			lbl.setText("Font color:");

			fontColorCombo = new Combo(container, SWT.READ_ONLY);
			String[] labels = new String[]{ "Based on heatmap color", "Black", "White" };
			fontColorCombo.setItems(labels);
			fontColorCombo.select(config.getFontColor()-1);
			GridDataFactory.fillDefaults().span(2,1).grab(true, false).applyTo(fontColorCombo);

			setTitle(getName());
			setMessage("Select the labels to display on the grid.");
			return area;
		}

		@Override
		protected void okPressed() {
			selectedFontColor = fontColorCombo.getSelectionIndex() + 1;
			applySettings(getLayerSupport().getViewer(), ValueLayer.this);
			super.okPressed();
		}

		@Override
		public void applySettings(GridViewer viewer, IGridLayer layer) {
			ValueConfig cfg = (ValueConfig)layer.getConfig();
			for (int i = 0; i < selectedValues.length; i++) cfg.setValueKey(i, selectedValues[i], getId());
			cfg.setFontColor(selectedFontColor, getId());
			viewer.getGrid().redraw();
		}
	}
}