package eu.openanalytics.phaedra.base.ui.volumerenderer.internal;

import org.eclipse.jface.viewers.ISelection;

public interface IDataModel {

	/**
	 * Handle an incoming (from another view/part) selection.
	 * 
	 * @param sel The incoming selection.
	 */
	public void handleExternalSelection(ISelection sel);
	
	/**
	 * Handle an outgoing (from within the spatial view) selection.
	 * 
	 * @param objectNames The names of the selected objects.
	 * @return An ISelection object containing the selected objects,
	 * ready to be fired to the platform.
	 */
	public ISelection handleInternalSelection(int[] objectNames);
	
	/**
	 * Get an instance of an IDataModelRenderer that can render this
	 * type of data model.
	 * 
	 * @return An IDataModelRenderer capable of rendering this model.
	 */
	public IDataModelRenderer getRenderer();
}
