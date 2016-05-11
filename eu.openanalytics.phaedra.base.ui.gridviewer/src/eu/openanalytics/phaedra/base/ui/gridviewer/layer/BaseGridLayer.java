package eu.openanalytics.phaedra.base.ui.gridviewer.layer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;

/**
 * Base implementation of a layer that is loaded via a {@link GridLayerSupport}.
 * 
 */
public abstract class BaseGridLayer implements IGridLayer {

	private String id;
	private int position;
	private ImageDescriptor imageDescriptor;
	private boolean enabled;
	private boolean defaultEnabled;
	private boolean initializationRequired;
	
	private GridLayerSupport layerSupport;
	private Object currentInput;
	
	@Override
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return imageDescriptor;
	}
	
	public void setImageDescriptor(ImageDescriptor imageDescriptor) {
		this.imageDescriptor = imageDescriptor;
	}
	
	@Override
	public boolean isEnabled() {
		return enabled;
	}
	
	@Override
	public void toggleEnabled(boolean enabled) {
		GridState.saveValue(GridState.ALL_PROTOCOLS, getId(), GridState.DEFAULT_ENABLED, enabled);
		this.enabled = enabled;
		
		if (enabled && initializationRequired) {
			setInput(currentInput);
			initializationRequired = false;
		}
	}
	
	@Override
	public boolean isDefaultEnabled() {
		return defaultEnabled;
	}
	
	public void setDefaultEnabled(boolean defaultEnabled) {
		this.defaultEnabled = defaultEnabled;
	}
	
	@Override
	public void setInput(Object newInput) {
		currentInput = newInput;
		if (enabled) {
			initialize();
		} else {
			initializationRequired = true;
		}
	}

	@Override
	public void update(GridCell cell, Object modelObject) {
		// Default: do nothing.
	}
	
	@Override
	public boolean hasConfigDialog() {
		// Default: layer has no configuration dialog.
		return false;
	}
	
	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		// Default: layer has no configuration dialog.
		return null;
	}
	
	@Override
	public void dispose() {
		// Default: do nothing.
	}
	
	@Override
	public void setConfig(Object config) {
		// Default: Do nothing.
	}
	
	@Override
	public Object getConfig() {
		// Default: layer has no config.
		return null;
	}
	
	@Override
	public boolean isRendering() {
		return false;
	}
	
	public GridLayerSupport getLayerSupport() {
		return layerSupport;
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	/**
	 * <p>This method is called when the layer receives new input (via {@link #setInput(Object)}.
	 * If the layer is not enabled at that time, it will initialize as soon as it is enabled
	 * (via {@link #toggleEnabled(boolean)}.</p>
	 * 
	 * This method should do things like registering listeners, retrieving state from the workbench, etc.
	 */
	protected abstract void initialize();
	
	protected Object getCurrentInput() {
		return currentInput;
	}
	
	/* package */ void setLayerSupport(GridLayerSupport layerSupport) {
		this.layerSupport = layerSupport;
	}
}
