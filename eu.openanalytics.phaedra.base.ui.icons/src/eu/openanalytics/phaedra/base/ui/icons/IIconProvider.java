package eu.openanalytics.phaedra.base.ui.icons;

import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.resource.ImageDescriptor;

public interface IIconProvider<T> extends IExecutableExtension {
	
	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".iconProvider";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	
	public String getId();	
	public Class<T> getType();
	public ImageDescriptor getDefaultImageDescriptor();
	public ImageDescriptor getCreateImageDescriptor();
	public ImageDescriptor getDeleteImageDescriptor();
	public ImageDescriptor getUpdateImageDescriptor();
}
