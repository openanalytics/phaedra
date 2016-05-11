package eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyResolver;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;

import ca.odell.glazedlists.SortedList;

public class CustomGlazedListsSortModel<T> extends GlazedListsSortModel<T> {

	public CustomGlazedListsSortModel(SortedList<T> sortedList, IColumnPropertyAccessor<T> columnPropertyAccessor
			, IConfigRegistry configRegistry, ILayer dataLayer) {

		super(sortedList, columnPropertyAccessor, columnPropertyAccessor, configRegistry, dataLayer);
	}

	public CustomGlazedListsSortModel(SortedList<T> sortedList, IColumnAccessor<T> columnAccessor
			, IColumnPropertyResolver columnPropertyResolver, IConfigRegistry configRegistry, ILayer dataLayer) {

		super(sortedList, columnAccessor, columnPropertyResolver, configRegistry, dataLayer);
	}

	public IColumnAccessor<T> getColumnAccessor() {
		return columnAccessor;
	}

}
