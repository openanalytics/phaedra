package eu.openanalytics.phaedra.base.ui.nattable.layer;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsStaticFilterStrategy;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowDataLayer;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowHeaderComposite;
import org.eclipse.nebula.widgets.nattable.filterrow.event.FilterAppliedEvent;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultColumnHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.Matcher;
import eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.CustomGlazedListsSortModel;
import eu.openanalytics.phaedra.base.ui.nattable.misc.PhaedraGlazedListsStaticFilterStrategy;

public class FullFeaturedColumnHeaderLayerStack<T> extends AbstractLayerTransform {

	private final FilterRowDataLayer<T> filterDataLayer;
	private final DefaultGlazedListsStaticFilterStrategy<T> filterStrategy;
	private final SelectionLayer selectionLayer;

	private final ColumnHeaderDataProvider<T> columnHeaderDataProvider;
	private final DataLayer columnHeaderDataLayer;
	private final ColumnHeaderLayer columnHeaderLayer;
	private final SortHeaderLayer<T> sortableColumnHeaderLayer;
	private final ColumnGroupHeaderLayer columnGroupHeaderLayer;

	public FullFeaturedColumnHeaderLayerStack(
			IColumnPropertyAccessor<T> columnAccessor, SortedList<T> sortedList, FilterList<T> filterList, ILayer bodyLayer
			, SelectionLayer selectionLayer, ColumnGroupModel columnGroupModel, IConfigRegistry configRegistry) {

		this.selectionLayer = selectionLayer;

		columnHeaderDataProvider = new ColumnHeaderDataProvider<>(columnAccessor);

		columnHeaderDataLayer = new DefaultColumnHeaderDataLayer(columnHeaderDataProvider);

		columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, bodyLayer, selectionLayer);

		GlazedListsSortModel<T> sortModel = new CustomGlazedListsSortModel<T>(
			sortedList,
			columnAccessor,
			configRegistry,
			columnHeaderDataLayer
		);
		sortableColumnHeaderLayer = new SortHeaderLayer<T>(columnHeaderLayer, sortModel, false);

		columnGroupHeaderLayer = new ColumnGroupHeaderLayer(sortableColumnHeaderLayer, selectionLayer, columnGroupModel);

		filterStrategy = new PhaedraGlazedListsStaticFilterStrategy<T>(
			filterList,
			columnAccessor,
			configRegistry
		);
		FilterRowHeaderComposite<T> composite = new FilterRowHeaderComposite<T>(
				filterStrategy, columnGroupHeaderLayer, columnHeaderDataProvider, configRegistry);

		filterDataLayer = composite.getFilterRowDataLayer();

		setUnderlyingLayer(composite);
	}

	public SelectionLayer getSelectionLayer() {
		return selectionLayer;
	}

	public ColumnHeaderDataProvider<T> getColumnHeaderDataProvider() {
		return columnHeaderDataProvider;
	}

	public DataLayer getColumnHeaderDataLayer() {
		return columnHeaderDataLayer;
	}

	public ColumnHeaderLayer getColumnHeaderLayer() {
		return columnHeaderLayer;
	}

	public SortHeaderLayer<T> getSortableColumnHeaderLayer() {
		return sortableColumnHeaderLayer;
	}

	public ColumnGroupHeaderLayer getColumnGroupHeaderLayer() {
		return columnGroupHeaderLayer;
	}

	public void addStaticFilter(Matcher<T> matcher) {
		filterStrategy.addStaticFilter(matcher);
		fireLayerEvent(new FilterAppliedEvent(this));
	}

	public void removeStaticFilter(Matcher<T> matcher) {
		filterStrategy.removeStaticFilter(matcher);
		filterStrategy.applyFilter(filterDataLayer.getFilterRowDataProvider().getFilterIndexToObjectMap());
		fireLayerEvent(new FilterAppliedEvent(this));
	}

	public void applyFilter() {
		filterStrategy.applyFilter(filterDataLayer.getFilterRowDataProvider().getFilterIndexToObjectMap());
		fireLayerEvent(new FilterAppliedEvent(this));
	}

}