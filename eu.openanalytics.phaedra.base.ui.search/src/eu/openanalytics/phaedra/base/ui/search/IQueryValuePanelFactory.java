package eu.openanalytics.phaedra.base.ui.search;

import java.util.Collection;

import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.editor.QueryEditor;

/**
 * Interface for a value input UI component factory for user input in a query filter.
 * Filters do not have to be complete to be applied. Here is an overview of the rules:
 * <li>The most precedence is given to filters with a matching type, columnName, operator type and operator (specific panels).
 * <li>If not found a filter is searched with a matching operator type and operator.
 * <li>When no found a general filter is searched with a matching operator.
 */
public interface IQueryValuePanelFactory extends IExecutableExtension {
	
	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".queryValuePanelFactory";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	
	/**
	 * The id of the factory
	 * @return
	 */
	public String getId();
	
	/**
	 * A set of filters for which this component factory is applicable. 
	 * @return
	 */
	public Collection<QueryFilter> getFilters();
	
	/**
	 * Returns <code>true</code> if the current value is reusable for this factory.
	 * @param queryFilter
	 * @return
	 */
	public boolean checkValue(QueryFilter queryFilter);
	
	/**
	 * Clears the value from the component.
	 * @param queryFilter
	 */
	public void clearValue(QueryFilter queryFilter);
	
	/**
	 * Returns a new composite which will be an input component for the value.
	 * @param parent
	 * @param queryEditor
	 * @param queryFilter
	 * @return
	 */
	public Composite createQueryValuePanel(Composite parent, QueryEditor queryEditor, QueryFilter queryFilter);
}
