package eu.openanalytics.phaedra.base.ui.editor;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.db.IValueObject;

/**
 * This class is used in combination with VOEditorInput when the list of input objects
 * is not known at instantiation time, or changes dynamically.
 * 
 * The input objects are determined at any given time by the collect() method.
 */
public interface IDynamicVOCollector {

	public String getEditorName();
	
	public String getVOClassName();
	
	public ImageDescriptor getImageDescriptor();
	
	public List<IValueObject> collect();
	
}
