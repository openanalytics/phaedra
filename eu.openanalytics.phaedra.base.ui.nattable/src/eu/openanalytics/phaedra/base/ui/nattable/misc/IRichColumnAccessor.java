package eu.openanalytics.phaedra.base.ui.nattable.misc;

import java.util.Map;

import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;

import eu.openanalytics.phaedra.base.ui.nattable.columnChooser.IColumnMatcher;
import eu.openanalytics.phaedra.base.ui.nattable.misc.NatTableToolTip.ITooltipColumnAccessor;

public interface IRichColumnAccessor<T> extends IColumnPropertyAccessor<T>, ITooltipColumnAccessor<T> {

	public int[] getColumnWidths();

	public Map<int[], AbstractCellPainter> getCustomCellPainters();

	public IConfiguration getCustomConfiguration();

	public Map<String, IColumnMatcher> getColumnDialogMatchers();

}
