package eu.openanalytics.phaedra.base.ui.gridviewer.layer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;

/**
 * GridLayerSupport that supports layer configuration for a GridViewer,
 * but in addition also links with a number of 'secondary' GridViewers.
 * 
 * The following items are still specific to each GridViewer:
 * <ul>
 * <li>Input object</li>
 * <li>LabelProvider</li>
 * <li>Layer instances</li>
 * </ul>
 * 
 * The following items are shared among the GridViewers:
 * <ul>
 * <li>Context menu</li>
 * <li>Layer toggle on/off</li>
 * <li>Layer configuration</li>
 * <li>Dispose</li>
 * </ul>
 */
public class MultiGridLayerSupport extends GridLayerSupport {

	private List<GridLayerSupport> delegateLayerSupports;

	public MultiGridLayerSupport(String id, GridViewer viewer) {
		super(id, viewer);
		delegateLayerSupports = new ArrayList<>();
	}

	public void linkViewer(GridViewer viewer) {
		GridLayerSupport support = new GridLayerSupport(getId(), viewer);
		support.setAttribute("featureProvider", getAttribute("featureProvider"));
		support.setAttribute(IS_HIDDEN, getAttribute(IS_HIDDEN));
		viewer.setLabelProvider(support.createLabelProvider());
		delegateLayerSupports.add(support);
	}


	public void setInput(Object newInput, GridViewer viewer) {
		if (getViewer() == viewer) super.setInput(newInput);
		else {
			for (GridLayerSupport support: delegateLayerSupports) {
				if (support.getViewer() == viewer) support.setInput(newInput);	
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		for (GridLayerSupport s: delegateLayerSupports) s.dispose();
	}

	@Override
	public boolean isRendering() {
		for (GridLayerSupport support : delegateLayerSupports) {
			for (IGridLayer layer : support.getLayers()) {
				if (layer.isEnabled() && layer.isRendering()) return true;
			}
		}
		for (IGridLayer layer : getLayers()) {
			if (layer.isEnabled() && layer.isRendering()) return true;
		}
		return false;
	}

	public void contributeContextMenu(IMenuManager manager) {

		// Add a dropdown for selecting active layers.
		IMenuManager layersManager = new MenuManager("Layers");
		manager.add(layersManager);
		for (final IGridLayer layer: getLayers()) {
			Action action = new Action(layer.getName(), Action.AS_CHECK_BOX) {
				@Override
				public void run() {
					boolean newState = !layer.isEnabled();
					layer.toggleEnabled(newState);
					setChecked(newState);
					getViewer().getControl().redraw();

					// Toggle layer in all linked viewers too.
					for (GridLayerSupport support: delegateLayerSupports) {
						support.getLayers().get(getLayers().indexOf(layer)).toggleEnabled(newState);
						support.getViewer().getControl().redraw();
					}
				}
			};
			if (layer.getImageDescriptor() != null) {
				if (!layer.isEnabled()) action.setImageDescriptor(layer.getImageDescriptor());
			}
			action.setChecked(layer.isEnabled());
			layersManager.add(action);
		}

		// Add buttons for configuring active layers.
		for (final IGridLayer layer: getLayers()) {
			if (!layer.isEnabled() || !layer.hasConfigDialog()) continue;

			Action action = new Action("Configure: " + layer.getName(), Action.AS_PUSH_BUTTON) {
				@Override
				public void run() {
					ILayerConfigDialog dialog = layer.createConfigDialog(Display.getCurrent().getActiveShell());
					int r = dialog.open();
					if (r == Window.OK) {
						// Apply the same settings to the corresponding layer in linked viewers.
						for (GridLayerSupport support: delegateLayerSupports) {
							IGridLayer l = support.getLayers().get(getLayers().indexOf(layer));
							dialog.applySettings(support.getViewer(), l);
						}
					}
				}
			};
			if (layer.getImageDescriptor() != null) {
				action.setImageDescriptor(layer.getImageDescriptor());
			}
			manager.add(action);
		}
	}

	public void updateLayers() {
		getViewer().getControl().redraw();
		for (GridLayerSupport support: delegateLayerSupports) {
			for (int i = 0; i < getLayers().size(); i++) {
				if (getLayers().get(i).isEnabled()) {
					support.getLayers().get(i).toggleEnabled(true);
					support.getLayers().get(i).setConfig(getLayers().get(i).getConfig());
				} else {
					support.getLayers().get(i).toggleEnabled(false);
				}
			}
			support.getViewer().getControl().redraw();
		}
	}

}
