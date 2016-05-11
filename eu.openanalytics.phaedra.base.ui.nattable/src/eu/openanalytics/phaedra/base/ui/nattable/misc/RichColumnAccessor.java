package eu.openanalytics.phaedra.base.ui.nattable.misc;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;

import eu.openanalytics.phaedra.base.ui.nattable.columnChooser.IColumnMatcher;

public abstract class RichColumnAccessor<T> implements IRichColumnAccessor<T> {

	@Override
	public void setDataValue(T rowObject, int columnIndex, Object newValue) {
		// Do nothing.
	}

	@Override
	public String getTooltipText(T rowObject, int colIndex) {
		return null;
	}

	@Override
	public int[] getColumnWidths() {
		return new int[0];
	}

	@Override
	public Map<int[], AbstractCellPainter> getCustomCellPainters() {
		return new HashMap<>();
	}

	@Override
	public IConfiguration getCustomConfiguration() {
		return new AbstractRegistryConfiguration() {
			@Override
			public void configureRegistry(IConfigRegistry configRegistry) {
				// Do nothing.
			}
		};
	}

	@Override
	public Map<String, IColumnMatcher> getColumnDialogMatchers() {
		return new HashMap<>();
	}

}
