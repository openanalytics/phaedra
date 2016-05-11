package eu.openanalytics.phaedra.base.ui.charting.v2.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;

public abstract class AbstractFilter<ENTITY, ITEM> implements IFilter<ENTITY, ITEM> {

	protected static final String SELECTED_FILTERS = "SELECTED_FILTERS";

	private String group;
	private List<MenuItem> menuItems = new ArrayList<MenuItem>();
	private List<String> filterItems = new ArrayList<String>();
	private List<String> activeFilterItems = new ArrayList<String>();
	private IDataProvider<ENTITY, ITEM> dataProvider;
	private List<IFilterValueChangedListener> valueChangedListeners;

	public AbstractFilter(String group, IDataProvider<ENTITY, ITEM> dataProvider) {
		this.group = group;
		this.dataProvider = dataProvider;
	}

	@Override
	public void initialize(final Menu parent) {
		valueChangedListeners = new ArrayList<IFilterValueChangedListener>();

		doInitialize(parent);
	}

	public void doInitialize(final Menu parent) {
		// Filter item
		MenuItem groupFilterItem = new MenuItem(parent, SWT.CASCADE);
		Menu groupFilterMenu = new Menu(parent);
		groupFilterItem.setMenu(groupFilterMenu);
		groupFilterItem.setText(getGroup() + (isActive() ? " [Enabled]" : ""));

		// Sub filter items
		for (final String filterItem : getFilterItems()) {
			MenuItem item = new MenuItem(groupFilterMenu, SWT.CHECK);
			item.setText(filterItem);
			item.setSelection(getActiveFilterItems().contains(filterItem));
			item.addListener(SWT.Selection, e -> {
				applyFilterItem(filterItem);
				groupFilterItem.setText(getGroup() + (isActive() ? " [Enabled]" : ""));
			});
			menuItems.add(item);
		}
	}

	private void applyFilterItem(String filterItem) {
		// store active/inactive filter items
		if (getActiveFilterItems().contains(filterItem)) {
			getActiveFilterItems().remove(filterItem);
		} else {
			getActiveFilterItems().add(filterItem);
		}
		// do any specific actions (nested values etc)
		doApplyFilterItem(filterItem);
	}

	@Override
	public void doApplyFilterItem(String filterItem) {
		if (valueChangedListeners != null) {
			for (IFilterValueChangedListener listener : valueChangedListeners) {
				listener.onFilterValueChanged();
			}
		}
	}

	@Override
	public abstract void filter();

	@Override
	public void addValueChangedListener(IFilterValueChangedListener listener) {
		if (valueChangedListeners == null) {
			valueChangedListeners = new ArrayList<IFilterValueChangedListener>();
		}
		valueChangedListeners.add(listener);
	}

	/* getter and setters */
	@Override
	public boolean isActive() {
		return getActiveFilterItems() != null && !getActiveFilterItems().isEmpty();
	}

	@Override
	public List<String> getFilterItems() {
		return filterItems;
	}

	public void setFilterItems(List<String> filterItems) {
		this.filterItems = filterItems;
	}

	public List<String> getActiveFilterItems() {
		return this.activeFilterItems;
	}

	public void setActiveFilterItems(List<String> activeFilterItems) {
		this.activeFilterItems = activeFilterItems;
	}

	@Override
	public String getGroup() {
		return group;
	}

	public IDataProvider<ENTITY, ITEM> getDataProvider() {
		return dataProvider;
	}

	@Override
	public Object getProperties() {
		Map<String, List<String>> properties = new HashMap<>();
		properties.put(SELECTED_FILTERS, getActiveFilterItems());
		return properties;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void setProperties(Object o) {
		if (o instanceof HashMap<?, ?>) {
			if (((HashMap) o).containsKey(SELECTED_FILTERS)) {
				getActiveFilterItems().clear();
				List<String> activeFilterItems = (List<String>) ((HashMap) o).get(SELECTED_FILTERS);
				for (String activeFilter : activeFilterItems) {
					applyFilterItem(activeFilter);
				}
			}
		}
	}

}