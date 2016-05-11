package eu.openanalytics.phaedra.ui.plate.table;

import java.util.List;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.IStyle;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.themes.ColorUtil;

import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;

public class FeatureTextPainter extends TextPainter {

	private static boolean useColors;

	private IColorMethod colorMethod;
	private ColorStore colorStore;

	private ILayerCell currentCell;

	private Feature feature;
	private List<Plate> plates;

	static {
		useColors = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.WELL_TABLE_COLORS);
		IPropertyChangeListener listener = (event) -> {
			if (event.getProperty().equals(Prefs.WELL_TABLE_COLORS)) {
				useColors = (Boolean)event.getNewValue();
			}
		};
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(listener);
	}

	/**
	 * Draws the background color of the Feature color method.
	 *
	 * <strong>Needs to be disposed after use.</strong>
	 *
	 * @param columnAccessor
	 */
	public FeatureTextPainter(Feature feature, List<Plate> plates) {
		super(false, true, 3, false);

		this.feature = feature;
		this.plates = plates;
		this.colorStore = new ColorStore();
	}

	@Override
	public void paintCell(ILayerCell cell, GC gc, Rectangle rectangle, IConfigRegistry configRegistry) {
		this.currentCell = cell;
		super.paintCell(cell, gc, rectangle, configRegistry);
		this.currentCell = null;
	}

	/**
	 * Setup the GC by the values defined in the given cell style.
	 * @param gc
	 * @param cellStyle
	 */
	@Override
	public void setupGCFromConfig(GC gc, IStyle cellStyle) {
		Color fg = cellStyle.getAttributeValue(CellStyleAttributes.FOREGROUND_COLOR);
		Color bg = cellStyle.getAttributeValue(CellStyleAttributes.BACKGROUND_COLOR);
		Font font = cellStyle.getAttributeValue(CellStyleAttributes.FONT);

		if (useColors && currentCell != null) {
			checkColorMethod();
			RGB rgb = colorMethod.getColor(getCellValue(currentCell));
			if (rgb != null) {
				if (currentCell.getDisplayMode() == DisplayMode.SELECT) {
					fg = ColorUtils.getTextColor(ColorUtil.blend(rgb, bg.getRGB()));
				} else {
					fg = ColorUtils.getTextColor(rgb);
				}
			}
		}

		gc.setAntialias(GUIHelper.DEFAULT_ANTIALIAS);
		gc.setTextAntialias(GUIHelper.DEFAULT_TEXT_ANTIALIAS);
		gc.setFont(font);
		gc.setForeground(fg != null ? fg : GUIHelper.COLOR_LIST_FOREGROUND);
		gc.setBackground(bg != null ? bg : GUIHelper.COLOR_LIST_BACKGROUND);
	}

	@Override
	protected Color getBackgroundColour(ILayerCell cell, IConfigRegistry configRegistry) {
		if (useColors) {
			checkColorMethod();
			RGB rgb = colorMethod.getColor(getCellValue(cell));
			if (rgb != null) {
				if (cell.getDisplayMode() == DisplayMode.SELECT) {
					Color color = super.getBackgroundColour(cell, configRegistry);
					return colorStore.get(ColorUtil.blend(rgb, color.getRGB()));
				}
				return colorStore.get(rgb);
			}
		}
		return super.getBackgroundColour(cell, configRegistry);
	}

	public void dispose() {
		colorStore.dispose();
	}

	public void resetColorMethod() {
		colorMethod = null;
	}

	private void checkColorMethod() {
		if (colorMethod != null) return;
		colorMethod = ColorMethodFactory.createColorMethod(feature);
		colorMethod.initialize(ColorMethodFactory.createData(plates, feature, feature.getNormalization(), "mean"));
	}

	private Double getCellValue(ILayerCell cell) {
		Object value = cell.getDataValue();
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else if (value instanceof String) {
			String strVal = (String) value;
			if (NumberUtils.isNumeric(strVal)) {
				return Double.valueOf(strVal);
			}
		}
		return Double.NaN;
	}

}
