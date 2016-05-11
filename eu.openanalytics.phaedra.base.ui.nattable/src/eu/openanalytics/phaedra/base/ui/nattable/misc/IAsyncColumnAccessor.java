package eu.openanalytics.phaedra.base.ui.nattable.misc;

import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;

public interface IAsyncColumnAccessor<T> extends IColumnPropertyAccessor<T> {

	/**
	 * Turn on/off the async nature of this ColumnAccessor.
	 * Should only be turned of during a sort/filter.
	 */
	void setAsync(boolean isAsync);

}
