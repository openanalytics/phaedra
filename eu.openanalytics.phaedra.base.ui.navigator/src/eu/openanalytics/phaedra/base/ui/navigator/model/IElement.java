package eu.openanalytics.phaedra.base.ui.navigator.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;

public interface IElement extends IAdaptable {

	public String getName();
	public String getId();
	public String getParentId();
	public IGroup getParent();
	public String getTooltip(); 
	public String[] getDecorations();
	
	public ImageDescriptor getImageDescriptor();
	
	public Object getData();
}
