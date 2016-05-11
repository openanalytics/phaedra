package eu.openanalytics.phaedra.base.ui.gridviewer.layer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.gridviewer.Activator;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;

/**
 * A grid layer is a virtual layer added on top of a grid.
 * It offers the following functionality on top of a standard {@link Grid}:
 * <ul>
 * <li>Add an {@link IGridCellRenderer} on top of any existing renderers</li>
 * <li>Toggle the layer on/off</li>
 * <li>Configure the layer with a configuration dialog</li>
 * </ul>
 */
public interface IGridLayer {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".gridLayer";
	public final static String ATTR_ID = "id";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_GRID_ID = "gridId";
	public final static String ATTR_DEFAULT_ENABLED = "defaultEnabled";
	public final static String ATTR_ICON = "icon";
	public final static String ATTR_POSITION = "position";
	
	public String getId();
	public String getName();
	public ImageDescriptor getImageDescriptor();
	
	/**
	 * Get the position of the layer in the stack of layers.
	 * Layers with a low position number are 'further away' in the stack, near the bottom.
	 */
	public int getPosition();
	
	/**
	 * Check whether this layer is currently enabled.
	 */
	public boolean isEnabled();
	
	/**
	 * Check whether this layer is enabled by default.
	 */
	public boolean isDefaultEnabled();
	
	/**
	 * Toggle a layer on or off.
	 * Note that this only affects the layer's internal state. The grid must still be redrawn to see the effects.
	 */
	public void toggleEnabled(boolean enabled);
	
	/**
	 * Create a cell renderer for this layer.
	 */
	public IGridCellRenderer createRenderer();
	
	/**
	 * Called when the input of the {@link GridViewer} is being set.
	 */
	public void setInput(Object newInput);

	/**
	 * Called when the label provider of the {@link GridViewer} updates or refreshes one of its cells.
	 */
	public void update(GridCell cell, Object modelObject);
	
	/**
	 * Check whether this layer has a configuration dialog.
	 */
	public boolean hasConfigDialog();

	/**
	 * If this layer has a configuration dialog, create it under the given shell.
	 * Otherwise, return null.
	 */
	public ILayerConfigDialog createConfigDialog(Shell shell);
	
	/**
	 * Get the current configuration of this layer.
	 * This method is used to pass configuration from the layer to the config dialog (and back),
	 * and to save the state of the layer when the parent view is being saved.
	 */
	public Object getConfig();
	
	/**
	 * Set the configuration of this layer.
	 * This method is used to restore the state of the layer when the parent
	 * view is being restored (from a saved view).
	 */
	public void setConfig(Object config);
	
	/**
	 * Check whether this layer is currently in the process of rendering cells.
	 * This method is used to wait until a layer is completely rendered (e.g. in reporting).
	 */
	public boolean isRendering();
	
	/**
	 * Release any resources this layer was using.
	 * IMPORTANT: after this method is called, the layer should be 'reusable'.
	 * I.e. this method is also called when a layer is toggled off.
	 */
	public void dispose();
}
