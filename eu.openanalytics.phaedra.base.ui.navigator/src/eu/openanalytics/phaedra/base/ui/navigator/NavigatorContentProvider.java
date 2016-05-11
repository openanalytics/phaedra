package eu.openanalytics.phaedra.base.ui.navigator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.Group;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;
import eu.openanalytics.phaedra.base.util.CollectionUtils;

public class NavigatorContentProvider implements ITreeContentProvider {

	public final static IGroup ROOT_GROUP = new Group("Root", "root", null);
	
	private List<IElementProvider> providers;
	private Map<IElementProvider, String> providerUserModes;
	private List<Class<?>> providedElementClasses;
	
	public NavigatorContentProvider() {
		providers = new ArrayList<IElementProvider>();
		providerUserModes = new HashMap<IElementProvider, String>();
		providedElementClasses = new ArrayList<>();
	}
	
	@Override
	public Object[] getElements(Object inputElement) {
		// Get the first-level groups.
		return gatherChildren(ROOT_GROUP);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IGroup) {
			IGroup group = (IGroup)parentElement;
			return gatherChildren(group);
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		IElement e = (IElement)element;
		return e.getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof IGroup);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Reload the root group.
		providers.clear();
		loadContributedGroups();
	}

	@Override
	public void dispose() {
		// Do nothing.
	}
	
	public boolean providesElementClass(Class<?> clazz) {
		return providedElementClasses.contains(clazz);
	}
	
	public void initializeExpandedStates(TreeViewer viewer, Object parent) {
		Object[] elements = (parent == null) ? getElements(null) : getChildren(parent);
		for (Object element: elements) {
			if (element instanceof IGroup && ((IGroup)element).isStartExpanded()) {
				viewer.setExpandedState(element, true);
				initializeExpandedStates(viewer, element);
			}
		}
	}
	
	private IElement[] gatherChildren(IGroup parent) {
		List<IElement> elements = new ArrayList<IElement>();
		for (IElementProvider provider: providers) {
			try {
				IElement[] providedElements = provider.getChildren(parent);
				if (providedElements != null) {
					for (IElement e: providedElements) {
						elements.add(e);
						if (e.getData() != null) CollectionUtils.addUnique(providedElementClasses, e.getData().getClass());
					}
				}
			} catch (Throwable t) {
				// Provider failed to obtain children: ignore.
			}
		}
		Collections.sort(elements, new ElementNameComparator());
		
		for (IElement element : elements) {
			((Element) element).setParent(parent);
		}		
		
		return elements.toArray(new IElement[elements.size()]);
	}
	
	private void loadContributedGroups() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IElementProvider.EXT_POINT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IElementProvider.ATTR_CLASS);
				if (o instanceof IElementProvider) {
					IElementProvider provider = (IElementProvider)o;
					providers.add(provider);
					providerUserModes.put(provider, el.getAttribute(IElementProvider.ATTR_USER_MODE));
				}
			} catch (CoreException e) {
				// Invalid element provider: ignore.
			}
		}
	}
	
	private class ElementNameComparator implements Comparator<IElement> {
		@Override
		public int compare(IElement e1, IElement e2) {
			boolean isGroup1 = e1 instanceof IGroup;
			boolean isGroup2 = e2 instanceof IGroup;
			if (isGroup1 != isGroup2) {
				if (isGroup1) return -1;
				if (isGroup2) return 1;
			}
			if (e1 == null || e1.getName() == null) return -1;
			if (e2 == null || e2.getName() == null) return 1;
			return e1.getName().compareTo(e2.getName());
		}
	}
}
