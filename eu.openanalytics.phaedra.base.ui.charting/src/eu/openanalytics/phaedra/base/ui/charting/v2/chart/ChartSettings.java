package eu.openanalytics.phaedra.base.ui.charting.v2.chart;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
import org.starlink.topcat.gating.GateRenderSettings;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.TopcatViewStyles;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Style;

public class ChartSettings implements Serializable {

	private static final long serialVersionUID = -6102072588923840309L;

	private transient Map<String, Style> groupStyles = new HashMap<String, Style>();
	private Map<String, StyleSettings> groupStyleSettings;

	private int selectionOpacity = Activator.getDefault().getPreferenceStore().getInt(Prefs.SELECTION_OPACITY);
	private String defaultSymbolType = "";
	private int defaultSymbolSize = Activator.getDefault().getPreferenceStore().getInt(Prefs.SYMBOL_SIZE);
	private boolean showGridLines = true;
	private boolean showFog = true;
	private String viewStyle = TopcatViewStyles.DEFAULT;
	private boolean showSelectedOnly;
	private Color defaultColor = Prefs.getDefaultColor();
	private Color backgroundColor = new Color(0x00ffffff, true);
	private boolean backgroundTransparant = true;
	private boolean adjacentBars;
	private boolean bars;
	private boolean lines;
	private int numberOfBins = 20;
	private double binWidth = 1;
	private boolean logaritmic = false;
	private boolean normalized = true;
	private boolean cumulative = false;

	private List<AuxiliaryChartSettings> auxiliaryChartSettings;
	private GateRenderSettings gateSettings = new GateRenderSettings();
	private TooltipsSettings tooltipSettings = new TooltipsSettings();

	private Map<String, String> miscSettings;

	public ChartSettings() {
		auxiliaryChartSettings = new ArrayList<AuxiliaryChartSettings>();
		auxiliaryChartSettings.add(new AuxiliaryChartSettings());
		miscSettings = new HashMap<>();
	}

	public ChartSettings(ChartSettings settings) {
		groupStyles = settings.groupStyles;
		groupStyleSettings = settings.groupStyleSettings;

		selectionOpacity = settings.selectionOpacity;
		defaultSymbolType = settings.defaultSymbolType;
		defaultSymbolSize = settings.defaultSymbolSize;
		showGridLines = settings.showGridLines;
		showFog = settings.showFog;
		viewStyle = settings.viewStyle;
		showSelectedOnly = settings.showSelectedOnly;
		defaultColor = settings.defaultColor;
		backgroundColor = settings.backgroundColor;
		backgroundTransparant = settings.backgroundTransparant;
		adjacentBars = settings.adjacentBars;
		bars = settings.bars;
		lines = settings.lines;
		numberOfBins = settings.numberOfBins;
		binWidth = settings.binWidth;
		logaritmic = settings.logaritmic;
		normalized = settings.normalized;
		cumulative = settings.cumulative;

		auxiliaryChartSettings = settings.auxiliaryChartSettings;
		gateSettings = settings.gateSettings;
		tooltipSettings = settings.tooltipSettings;

		miscSettings = new HashMap<>();
		miscSettings.putAll(settings.getMiscSettings());
	}

	// Create Style from StyleSetting
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		groupStyles = new HashMap<String, Style>();
		if (groupStyleSettings != null) {
			for (Entry<String, StyleSettings> entry : groupStyleSettings.entrySet()) {
				groupStyles.put(entry.getKey(), entry.getValue().createStyle());
			}
		}
		// Support for older saved views.
		if (miscSettings == null) miscSettings = new HashMap<>();
	}

	/**
	 * Style is a Topcat object that is not Serializable,
	 * therefore we use a serializable StyleSetting object instead (String representation)
	 */
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		if (groupStyles != null) {
			groupStyleSettings = new HashMap<String, StyleSettings>();
			for (Entry<String, Style> entry : groupStyles.entrySet()) {
				groupStyleSettings.put(entry.getKey(), new StyleSettings(entry.getValue()));
			}
		}
		stream.defaultWriteObject();
	}

	public Style getStyle(String group) {
		return groupStyles.get(group);
	}

	public void putStyle(String group, Style style) {
		groupStyles.put(group, style);
	}

	public void resetStyles() {
		groupStyles.clear();
	}

	public int getSelectionOpacity() {
		return selectionOpacity;
	}

	public void setSelectionOpacity(int selectionOpacity) {
		this.selectionOpacity = selectionOpacity;
	}

	public String getDefaultSymbolType() {
		return defaultSymbolType;
	}

	public void setDefaultSymbolType(String defaultSymbolType) {
		this.defaultSymbolType = defaultSymbolType;
	}

	public int getDefaultSymbolSize() {
		return defaultSymbolSize;
	}

	public void setDefaultSymbolSize(int defaultSymbolSize) {
		this.defaultSymbolSize = defaultSymbolSize;
		if (groupStyles != null) {
			for (Style style : groupStyles.values()) {
				if (style instanceof MarkStyle) {
					((MarkStyle)style).setSize(defaultSymbolSize);
					((MarkStyle)style).setLineWidth(defaultSymbolSize);
				}
			}
		}
	}

	public boolean isShowGridLines() {
		return showGridLines;
	}

	public void setShowGridLines(boolean showGridlines) {
		this.showGridLines = showGridlines;
	}

	public Color getDefaultColor() {
		return defaultColor;
	}

	public void setDefaultColor(Color defaultColor) {
		this.defaultColor = defaultColor;
	}

	public int getOpacity() {
		return isShowSelectedOnly() ? 100 : getSelectionOpacity();
	}

	public boolean isShowSelectedOnly() {
		return showSelectedOnly;
	}

	public void setShowSelectedOnly(boolean showSelectedOnly) {
		this.showSelectedOnly = showSelectedOnly;
	}

	public List<AuxiliaryChartSettings> getAuxiliaryChartSettings() {
		return auxiliaryChartSettings;
	}

	public void setAuxiliaryChartSettings(List<AuxiliaryChartSettings> auxiliaryChartSettings) {
		this.auxiliaryChartSettings = auxiliaryChartSettings;
	}

	public boolean isShowFog() {
		return showFog;
	}

	public void setShowFog(boolean showFog) {
		this.showFog = showFog;
	}

	public String getViewStyle() {
		return viewStyle;
	}

	public void setViewStyle(String viewStyle) {
		this.viewStyle = viewStyle;
	}

	public GateRenderSettings getGateSettings() {
		return gateSettings;
	}

	public void setGateSettings(GateRenderSettings gateSettings) {
		this.gateSettings = gateSettings;
	}

	public TooltipsSettings getTooltipSettings() {
		return tooltipSettings;
	}

	public void setTooltipSettings(TooltipsSettings tooltipSettings) {
		this.tooltipSettings = tooltipSettings;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public boolean isBackgroundTransparant() {
		return backgroundTransparant;
	}

	public void setBackgroundTransparant(boolean backgroundTransparant) {
		this.backgroundTransparant = backgroundTransparant;
	}

	public boolean isAdjacentBars() {
		return adjacentBars;
	}

	public void setAdjacentBars(boolean adjacentBars) {
		this.adjacentBars = adjacentBars;
	}

	public boolean isBars() {
		return bars;
	}

	public void setBars(boolean bars) {
		this.bars = bars;
	}

	public double getBinWidth() {
		return binWidth;
	}

	public void setBinWidth(double binWidth) {
		this.binWidth = binWidth;
	}

	public boolean isLines() {
		return lines;
	}

	public void setLines(boolean lines) {
		this.lines = lines;
	}

	public boolean isLogaritmic() {
		return logaritmic;
	}

	public void setLogaritmic(boolean logaritmic) {
		this.logaritmic = logaritmic;
	}

	public boolean isNormalized() {
		return normalized;
	}

	public void setNormalized(boolean normalized) {
		this.normalized = normalized;
	}

	public boolean isCumulative() {
		return cumulative;
	}

	public void setCumulative(boolean cumulative) {
		this.cumulative = cumulative;
	}

	public int getNumberOfBins() {
		return numberOfBins;
	}

	public void setNumberOfBins(int numberOfBins) {
		this.numberOfBins = numberOfBins;
	}

	/*
	 * Misc settings
	 * *************
	 */

	public Map<String, String> getMiscSettings() {
		return miscSettings;
	}

	public String getStringMiscSetting(String name) {
		return miscSettings.get(name);
	}

	public int getIntMiscSetting(String name) {
		String value = miscSettings.get(name);
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return 0;
		}
	}

	public double getDoubleMiscSetting(String name) {
		String value = miscSettings.get(name);
		try {
			return Double.parseDouble(value);
		} catch (Exception e) {
			return 0;
		}
	}

	public RGB getRGBMiscSetting(String name) {
		String value = miscSettings.get(name);
		try {
			return StringConverter.asRGB(value);
		} catch (Exception e) {
			return null;
		}
	}

	public void setMiscSetting(String name, String value) {
		miscSettings.put(name, value);
	}

	public void setMiscSetting(String name, int value) {
		miscSettings.put(name, ""+value);
	}

	public void setMiscSetting(String name, double value) {
		miscSettings.put(name, ""+value);
	}

	public void setMiscSetting(String name, RGB value) {
		miscSettings.put(name, StringConverter.asString(value));
	}

}