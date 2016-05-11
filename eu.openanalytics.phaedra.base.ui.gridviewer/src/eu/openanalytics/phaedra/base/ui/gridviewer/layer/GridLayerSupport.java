package eu.openanalytics.phaedra.base.ui.gridviewer.layer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.provider.AbstractGridLabelProvider;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.DelegatingCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;

/**
 * This class adds layer support to a {@link GridViewer}.
 * All of the registered layer extensions are added to the grid.
 * After instantiation, {@link #createLabelProvider()} should be called and set to
 * the viewer.
 */
public class GridLayerSupport {
	
	public static final String IS_HIDDEN = "isHidden";

	private String id;
	private GridViewer viewer;
	private List<IGridLayer> layers;
	private DelegatingCellRenderer layeredRenderer;
	private Map<String, Object> attributesMap;

	public GridLayerSupport(String id, GridViewer viewer) {
		this.id = id;
		this.viewer = viewer;
		this.layers = new ArrayList<IGridLayer>();
		this.attributesMap = new HashMap<>();
		this.layeredRenderer = new DelegatingCellRenderer();
		
		setAttribute("stat", "mean");
		loadContributedLayers();
	}

	public String getId() {
		return id;
	}
	
	public void setInput(Object newInput) {
		for (IGridLayer layer: layers) {
			layer.setInput(newInput);
		}
		viewer.setInput(newInput);
	}

	public List<IGridLayer> getLayers() {
		return layers;
	}

	public GridViewer getViewer() {
		return viewer;
	}

	public IBaseLabelProvider createLabelProvider() {
		return new AbstractGridLabelProvider() {
			@Override
			public IGridCellRenderer createCellRenderer() {
				return layeredRenderer;
			}
			@Override
			public void update(GridCell cell, Object element) {
				for (IGridLayer layer: layers) {
					layer.update(cell, element);
				}
			}
		};
	}

	public void contributeContextMenu(IMenuManager manager) {
		// Add a dropdown for selecting active layers.
		IMenuManager layersManager = new MenuManager("Layers");
		manager.add(layersManager);
		for (final IGridLayer layer: layers) {
			Action action = new Action(layer.getName(), Action.AS_CHECK_BOX) {
				@Override
				public void run() {
					boolean newState = !layer.isEnabled();
					layer.toggleEnabled(newState);
					setChecked(newState);
					if (viewer != null) viewer.getControl().redraw();
				}
			};
			if (layer.getImageDescriptor() != null) {
				if (!layer.isEnabled()) action.setImageDescriptor(layer.getImageDescriptor());
			}
			action.setChecked(layer.isEnabled());
			layersManager.add(action);
		}

		// Add buttons for configuring active layers.
		for (final IGridLayer layer: layers) {
			if (!layer.isEnabled() || !layer.hasConfigDialog()) continue;

			Action action = new Action("Configure: " + layer.getName(), Action.AS_PUSH_BUTTON) {
				@Override
				public void run() {
					ILayerConfigDialog dialog = layer.createConfigDialog(Display.getCurrent().getActiveShell());
					dialog.open();
				}
			};
			if (layer.getImageDescriptor() != null) {
				action.setImageDescriptor(layer.getImageDescriptor());
			}
			manager.add(action);
		}
	}

	public boolean isRendering() {
		for (IGridLayer layer : layers) {
			if (layer.isEnabled() && layer.isRendering()) return true;
		}
		return false;
	}
	
	public void setAttribute(String name, Object value) {
		attributesMap.put(name, value);
	}

	public Object getAttribute(String name) {
		return attributesMap.get(name);
	}
	
	public void dispose() {
		for (IGridLayer layer: layers) {
			layer.dispose();
		}
	}

	/*
	 * setStat and getStat are used by multi-plate grid viewers that show
	 * e.g. mean values of the plates.
	 */
	
	public void setStat(String newStat) {
		String currentStat = getStat();
		if (newStat.equals(currentStat)) return;
		setAttribute("stat", newStat);
		setInput(getViewer().getInput());
	}

	public String getStat() {
		return (String) getAttribute("stat");
	}
	
	/*
	 * Non-public
	 * **********
	 */

	@SuppressWarnings("deprecation")
	private ImageDescriptor locateImageDescriptor(String iconName, IConfigurationElement configElement) {
		// If the icon attribute was omitted, use the default one
		if (iconName == null) {
			return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
					ISharedImages.IMG_DEF_VIEW);
		}
		IExtension extension = configElement.getDeclaringExtension();
		String extendingPluginId = extension.getNamespace();
		ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(extendingPluginId, iconName);
		// If the icon attribute was invalid, use the error icon
		if (imageDescriptor == null) {
			imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
		}

		return imageDescriptor;
	}
	
	private void loadContributedLayers() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IGridLayer.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				String gridId = el.getAttribute(IGridLayer.ATTR_GRID_ID);
				if (!getId().contains(gridId)) continue;

				Object o = el.createExecutableExtension(IGridLayer.ATTR_CLASS);
				if (o instanceof IGridLayer) {
					IGridLayer layer = (IGridLayer)o;
					if (layer instanceof BaseGridLayer) {
						BaseGridLayer baseLayer = (BaseGridLayer)layer;
						baseLayer.setId(el.getAttribute(IGridLayer.ATTR_ID));

						String iconPath = el.getAttribute(IGridLayer.ATTR_ICON);
						if (iconPath != null) {
							baseLayer.setImageDescriptor(locateImageDescriptor(iconPath, el));
						}

						String defaultEnabled = el.getAttribute(IGridLayer.ATTR_DEFAULT_ENABLED);
						if (defaultEnabled != null) {
							baseLayer.setDefaultEnabled(defaultEnabled.equalsIgnoreCase("true"));
						} else {
							baseLayer.setDefaultEnabled(true);
						}
						baseLayer.toggleEnabled(baseLayer.isDefaultEnabled());

						int position = 0;
						String positionAttr = el.getAttribute(IGridLayer.ATTR_POSITION);
						if (positionAttr != null) {
							position = Integer.parseInt(positionAttr);
						}
						baseLayer.setPosition(position);

						baseLayer.setLayerSupport(this);
					}
					getLayers().add(layer);
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}

		// Enforce order imposed by "position" attribute.
		Collections.sort(getLayers(), new Comparator<IGridLayer>() {
			@Override
			public int compare(IGridLayer o1, IGridLayer o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null && o2 != null) return -1;
				if (o1 != null && o2 == null) return 1;
				return o1.getPosition() - o2.getPosition();
			}
		});
		for (IGridLayer layer: getLayers()) {
			layeredRenderer.addDelegate(layer.createRenderer());
		}
	}
}