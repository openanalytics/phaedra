package eu.openanalytics.phaedra.base.ui.search;

import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.nebula.widgets.nattable.NatTable;

import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;

/**
 * This interface provides support for a specific type.
 */
public interface IQueryEditorSupport extends IExecutableExtension {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".queryEditorSupport";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";

	/**
	 * Returns the id of a supported type.
	 * @return
	 */
	public String getId();

	/**
	 * Gives the label of the supported type as shown in the UI.
	 * @return
	 */
	public String getLabel();

	/**
	 * Returns a label for a specific field in the UI.
	 * @param fieldName
	 * @return
	 */
	public String getLabelForField(String fieldName);

	/**
	 * Returns the type that is supported.
	 * @return
	 */
	public Class<?> getSupportedClass();

	/**
	 * Returns a new ColumnAccessor for a supported type.
	 * @return
	 */
	public IRichColumnAccessor<?> getColumnAccessor();

	/**
	 * Perform further customization on the result table.
	 * @param table
	 */
	public void customize(NatTable table);
	
}
