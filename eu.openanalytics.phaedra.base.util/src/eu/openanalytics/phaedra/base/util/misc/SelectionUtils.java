package eu.openanalytics.phaedra.base.util.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.util.IListAdaptable;

/**
 * A collection of utilities related to JFace selection events.
 */
public class SelectionUtils {

	/**
	 * Get the first element of a selection that matches the specified class.
	 * Both casting and adapting will be attempted.
	 */
	public static <E> E getFirstObject(ISelection selection, Class<E> clazz) {
		return getFirstObject(selection, clazz, true);
	}

	/**
	 * Get the first element of a selection that matches the specified class.
	 */
	public static <E> E getFirstObject(ISelection selection, Class<E> clazz, boolean allowAdapting) {
		E element = null;
		if (selection instanceof IStructuredSelection) {
			Object selectedItem = ((IStructuredSelection)selection).getFirstElement();
			if (selectedItem != null) element = getAsClass(selectedItem, clazz, allowAdapting);
		}
		return element;
	}

	/**
	 * Get the element of a selection of the size 1 that matches the specified class.
	 */
	public static <E> E getSingleObject(ISelection selection, Class<E> clazz, boolean allowAdapting) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection)selection;
			if (structuredSelection.isEmpty()) {
				return null;
			}
			if (structuredSelection.size() == 1) {
				return toSingleObject(structuredSelection.getFirstElement(), clazz, allowAdapting);
			}
			else if (allowAdapting) {
				Iterator<?> iter = structuredSelection.iterator();
				E single = toSingleObject(iter.next(), clazz, allowAdapting);
				if (single == null) {
					return null;
				}
				while (iter.hasNext()) {
					E e = toSingleObject(iter.next(), clazz, allowAdapting);
					if (!single.equals(e)) {
						return null;
					}
				}
				return single;
			}
		}
		return null;
	}
	
	private static <E> E toSingleObject(Object o, Class<E> clazz, boolean allowAdapting) {
		if (o == null) {
			return null;
		}
		if (clazz.isInstance(o)) {
			return (E)o;
		}
		if (allowAdapting && o instanceof IAdaptable) {
			if (o instanceof IListAdaptable) {
				List<E> eList = ((IListAdaptable)o).getAdapterList(clazz);
				if (eList != null) {
					return (eList.size() == 1) ? eList.get(0) : null;
				}
			}
			return ((IAdaptable)o).getAdapter(clazz);
		}
		return null;
	}

	/**
	 * Get all elements of a selection that match the specified class.
	 * Both casting and adapting will be attempted.
	 */
	public static <E> List<E> getObjects(ISelection selection, Class<E> clazz) {
		return getObjects(selection, clazz, true);
	}

	/**
	 * Get all elements of a selection that match the specified class.
	 */
	public static <E> List<E> getObjects(ISelection selection, Class<E> clazz, boolean allowAdapting) {
		Set<E> objects = new LinkedHashSet<>();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection struct = ((IStructuredSelection)selection);
			if (!struct.isEmpty()) {
				for (Iterator<?> it = struct.iterator(); it.hasNext();) {
					Object o = it.next();
					if (o == null) {
						continue;
					}
					if (clazz.isInstance(o)) {
						objects.add((E)o);
						continue;
					}
					if (allowAdapting && o instanceof IAdaptable) {
						if (o instanceof IListAdaptable) {
							List<E> eList = ((IListAdaptable)o).getAdapterList(clazz);
							if (eList != null) {
								objects.addAll(eList);
								continue;
							}
						}
						E e = ((IAdaptable)o).getAdapter(clazz);
						if (e != null) {
							objects.add(e);
							continue;
						}
					}
				}
			}
		}
		return new ArrayList<>(objects);
	}

	/**
	 * Attempt to convert the given object to the given class.
	 * Both casting and adapting will be attempted.
	 */
	public static <E> E getAsClass(Object o, Class<E> clazz) {
		return getAsClass(o, clazz, true);
	}

	/**
	 * Attempt to convert the given object to the given class.
	 */
	@SuppressWarnings("unchecked")
	public static <E> E getAsClass(Object o, Class<E> clazz, boolean allowAdapting) {
		if (clazz.isAssignableFrom(o.getClass())) {
			return (E)o;
		} else if (allowAdapting && o instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable)o;
			E e = adaptable.getAdapter(clazz);
			return e;
		}
		return null;
	}

	/**
	 * Attempt to convert the given array of objects to the given class.
	 * Both casting and adapting will be attempted.
	 */
	public static <E> List<E> getAsClass(Object[] objects, Class<E> clazz) {
		List<E> results = new ArrayList<>();
		if (objects == null) return results;
		for (Object o: objects) {
			E match = getAsClass(o, clazz, true);
			if (match != null) results.add(match);
		}
		return results;
	}

	/**
	 * Attempt to convert the given collection of objects to the given class.
	 * Both casting and adapting will be attempted.
	 */
	public static <E> List<E> getAsClass(Collection<?> o, Class<E> clazz) {
		List<E> results = new ArrayList<>();
		if (o == null || o.isEmpty()) return results;
		Iterator<?> it = o.iterator();
		while (it.hasNext()) {
			E match = getAsClass(it.next(), clazz, true);
			if (match != null) results.add(match);
		}
		return results;
	}

	/**
	 * Get the first element of the collection that matches the given class.
	 * Both casting and adapting will be attempted.
	 * Note: if the first element doesn't match, the next will be attempted.
	 */
	public static <E> E getFirstAsClass(Collection<?> o, Class<E> clazz) {
		if (o == null || o.isEmpty()) return null;
		Iterator<?> it = o.iterator();
		while (it.hasNext()) {
			E match = getAsClass(it.next(), clazz, true);
			if (match != null) return match;
		}
		return null;
	}

	/**
	 * Obtain an initial selection for the given listeners.
	 * The initial selection will be retrieved from the active workbench part, if any.
	 */
	public static void triggerActiveSelection(ISelectionListener... listeners) {
		try {
			ISelection startingSelection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();
			if (startingSelection != null) {
				for (ISelectionListener listener : listeners) {
					listener.selectionChanged(null, startingSelection);
				}
			}
		} catch (Throwable t) {
			// No active page or invalid selection object.
		}
	}

	/**
	 * Obtain an initial selection for the given listeners.
	 * The initial selection will be retrieved from the active editor, if any.
	 */
	public static void triggerActiveEditorSelection(ISelectionListener... listeners) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page == null) return;
			IEditorPart editor = page.getActiveEditor();
			if (editor != null) {
				ISelectionProvider provider = editor.getEditorSite().getSelectionProvider();
				if (provider != null && provider.getSelection() != null && !provider.getSelection().isEmpty()) {
					for (ISelectionListener listener : listeners) {
						listener.selectionChanged(editor, provider.getSelection());
					}
				} else if (editor.getEditorInput() != null) {
					for (ISelectionListener listener : listeners) {
						// IEditorInput is adaptable, so this could be an appropriate object to trigger.
						listener.selectionChanged(editor, new StructuredSelection(editor.getEditorInput()));
					}
				}
			}
		} catch (Throwable t) {
			// No active page or invalid selection object.
		}
	}
}
