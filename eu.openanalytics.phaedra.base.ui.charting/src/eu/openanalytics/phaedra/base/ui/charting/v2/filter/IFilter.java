package eu.openanalytics.phaedra.base.ui.charting.v2.filter;

import java.util.List;

import org.eclipse.swt.widgets.Menu;

public interface IFilter<ENTITY, ITEM> {

	boolean isActive();

	String getGroup();

	void initialize(final Menu parent);

	void filter();

	void addValueChangedListener(IFilterValueChangedListener filterValueChangedListener);

	void doApplyFilterItem(String string);

	List<String> getFilterItems();

	Object getProperties();

	void setProperties(Object o);

}