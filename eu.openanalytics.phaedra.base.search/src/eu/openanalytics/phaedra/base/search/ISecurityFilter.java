package eu.openanalytics.phaedra.base.search;

import org.eclipse.core.runtime.IExecutableExtension;

import eu.openanalytics.phaedra.base.search.model.QueryFilter;

/**
 * Security filter. This is an internal filter which hides results that the user should not see.
 */
public interface ISecurityFilter extends IExecutableExtension {
	
	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".securityFilter";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	
	/**
	 * Returns the id of the filter.
	 * @return
	 */
	public String getId();
	
	/**
	 * Returns a filter that will be added to the list of filters specified by the user if applicable.
	 * @return
	 */
	public QueryFilter getInternalFilter();
}
