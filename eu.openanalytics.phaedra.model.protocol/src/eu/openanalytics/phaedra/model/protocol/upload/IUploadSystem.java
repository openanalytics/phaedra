package eu.openanalytics.phaedra.model.protocol.upload;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.model.protocol.Activator;

public interface IUploadSystem {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".uploadSystem";
	public final static String ATTR_CLASS = "class";
	
	public String getName();
	
	public ImageDescriptor getIcon();

}
