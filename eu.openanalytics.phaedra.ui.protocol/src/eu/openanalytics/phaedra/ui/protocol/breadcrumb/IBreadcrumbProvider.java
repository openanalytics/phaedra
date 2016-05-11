package eu.openanalytics.phaedra.ui.protocol.breadcrumb;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Table;

import eu.openanalytics.phaedra.ui.protocol.Activator;

public interface IBreadcrumbProvider {

	public static final String EXT_PT_ID = Activator.PLUGIN_ID + ".breadcrumbProvider";
	public static final String ATTR_CLASS = "class";

	public void addMenuContribution(Object o, Table table, TreePath path);
	
}