package eu.openanalytics.phaedra.base.ui.nattable.painter;

import java.util.function.IntFunction;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

/**
 * Paints a small (16x16) flag icon to represent a numerical value.
 * For example:
 * <ul>
 * <li>Negative value: red flag</li>
 * <li>Zero value: white flag</li>
 * <li>Positive value: green flag</li>
 * </ul>
 * To configure the value -> flag lookup, provide one or more {@link FlagMapping} objects.
 */
public class FlagCellPainter extends ImageTextPainter {

	private String baseName;
	private FlagMapping[] mappings;

	public FlagCellPainter(FlagMapping... mappings) {
		this("flag", mappings);
	}

	public FlagCellPainter(String base, FlagMapping... mappings) {
		this.baseName = base;
		if (mappings.length == 0) mappings = getDefaultMappings();
		this.mappings = mappings;
	}

	@Override
	public void paintCell(ILayerCell cell, GC gc, Rectangle adjustedCellBounds, IConfigRegistry configRegistry) {
		paintBackground(cell, gc, adjustedCellBounds, configRegistry);

		int value = 0;
		Object valueObject = cell.getDataValue();

		if (valueObject instanceof Boolean) {
			value = (Boolean)valueObject ? 1 : 0;
		} else if (valueObject instanceof Number) {
			value = ((Number)valueObject).intValue();
		}

		Image icon = null;
		for (FlagMapping mapping: mappings) {
			if (mapping.filter.matches(value)) {
				icon = IconManager.getIconImage(baseName + mapping.icon);
				break;
			}
		}
		if (icon != null) {
			int centerX = adjustedCellBounds.x + adjustedCellBounds.width/2;
			int centerY = adjustedCellBounds.y + adjustedCellBounds.height/2;
			Rectangle bounds = icon.getBounds();
			gc.drawImage(icon, centerX - bounds.width/2, centerY - bounds.height/2);
		}
	}

	public static FlagMapping[] getDefaultMappings() {
		return new FlagMapping[] {
				new FlagMapping(FlagFilter.Negative, Flag.Red),
				new FlagMapping(FlagFilter.Zero, Flag.White),
				new FlagMapping(FlagFilter.One, Flag.Blue),
				new FlagMapping(FlagFilter.GreaterThanOne, Flag.Green)
		};
	}

	public static enum FlagFilter {

		Negative(i -> i<0),
		NegativeOrZero(i -> i<=0),
		Zero(i -> i==0),
		One(i -> i==1),
		GreaterThanOne(i -> i>1),
		Positive(i -> i>0),
		PositiveOrZero(i -> i>=0),
		All(i -> true);

		private IntFunction<Boolean> matcher;

		private FlagFilter(IntFunction<Boolean> matcher) {
			this.matcher = matcher;
		}

		public boolean matches(int value) {
			return matcher.apply(value);
		}
	}

	public static enum Flag {

		Red("_red.png"),
		White("_white.png"),
		Green("_green.png"),
		Blue("_blue.png");

		private String icon;

		private Flag(String icon) {
			this.icon = icon;
		}

		public String getIcon() {
			return icon;
		}
	}

	public static class FlagMapping {

		public FlagFilter filter;
		public String icon;

		public FlagMapping(FlagFilter filter, String iconName) {
			this.filter = filter;
			this.icon = iconName;
		}

		public FlagMapping(FlagFilter filter, Flag flag) {
			this.filter = filter;
			this.icon = flag.getIcon();
		}

	}
}
