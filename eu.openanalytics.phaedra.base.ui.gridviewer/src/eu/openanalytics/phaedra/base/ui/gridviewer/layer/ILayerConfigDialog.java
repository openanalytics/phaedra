package eu.openanalytics.phaedra.base.ui.gridviewer.layer;

import org.eclipse.jface.dialogs.Dialog;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;

/**
 * This interface represents a grid layer configuration dialog.
 * The dialog is responsible from retrieving (upon opening) and updating (upon clicking Ok) configuration state in the grid layer.
 */
public interface ILayerConfigDialog {

	/**
	 * Open the dialog and block until the user clicks Ok or Cancel.
	 */
	public int open();

	/**
	 * This method is called by MultiGridLayerSupport in order to apply the same configuration
	 * to multiple grid viewers.
	 * It may also be called by the dialog itself during {@link Dialog#okPressed} to update the layer's configuration
	 * more easily.
	 */
	public void applySettings(GridViewer viewer, IGridLayer layer);
}
