package eu.openanalytics.phaedra.base.ui.navigator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.interaction.ElementHandlerRegistry;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.Group;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;

public class Navigator extends ViewPart {

	private TreeViewer treeViewer;
	
	private IModelEventListener eventListener;
	
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout(SWT.VERTICAL));

		treeViewer = new TreeViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);

		treeViewer.setContentProvider(new NavigatorContentProvider());
		treeViewer.setLabelProvider(new NavigatorLabelProvider());
		treeViewer.setInput(NavigatorContentProvider.ROOT_GROUP);
		((NavigatorContentProvider)treeViewer.getContentProvider()).initializeExpandedStates(treeViewer, null);
		ColumnViewerToolTipSupport.enableFor(treeViewer, ToolTip.NO_RECREATE);

		// Drag & Drop Support
		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transferTypes = new Transfer[] { LocalSelectionTransfer.getTransfer(), PluginTransfer.getInstance() };
		treeViewer.addDragSupport(operations, transferTypes, new DragSourceListener() {
			@Override
			public void dragStart(DragSourceEvent event) {
				// The drag is canceled by default.
				event.doit = false;
				event.detail = DND.DROP_NONE;
				
				TreeSelection sel = (TreeSelection)treeViewer.getSelection();
				IElement[] elements = new IElement[sel.size()];
				SelectionUtils.getObjects(sel, IElement.class).toArray(elements);
				ElementHandlerRegistry.getInstance().dragStart(elements, event);
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				TreeSelection sel = (TreeSelection)treeViewer.getSelection();
				IElement[] elements = new IElement[sel.size()];
				SelectionUtils.getObjects(sel, IElement.class).toArray(elements);
				ElementHandlerRegistry.getInstance().dragSetData(elements, event);
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				TreeSelection sel = (TreeSelection)treeViewer.getSelection();
				IElement[] elements = new IElement[sel.size()];
				SelectionUtils.getObjects(sel, IElement.class).toArray(elements);
				ElementHandlerRegistry.getInstance().dragFinished(elements, event);
			}
		});
		treeViewer.addDropSupport(operations, transferTypes, new ViewerDropAdapter(treeViewer) {
			private IElement element;
			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				if (target == null) return false;
				element = (IElement) target;
				return ElementHandlerRegistry.getInstance().validateDrop(element, operation, transferType);
			}

			@Override
			public boolean performDrop(Object data) {
				if (data != null && element != null) {
					return ElementHandlerRegistry.getInstance().performDrop(element, data);
				}
				return false;
			}
		});

		getSite().setSelectionProvider(treeViewer);

		createToolbar();
		createContextMenu();
		hookDoubleClickAction();
		initListeners();
		
		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewNavigator");
		
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		ModelEventService.getInstance().removeEventListener(eventListener);
		super.dispose();
	}

	public void refreshTree(ModelEvent event) {
		Runnable r = new RefreshTreeCallback(event);
		if (Display.getCurrent() == null) {
			Display.getDefault().asyncExec(r);
		} else {
			r.run();
		}
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void createToolbar() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mgr = bars.getToolBarManager();

		Action collapseAllAction = new Action() {
			@Override
			public void run() {
				treeViewer.collapseAll();
			}
		};
		collapseAllAction.setToolTipText("Collapse All");
		collapseAllAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));
		mgr.add(collapseAllAction);

		Action refreshAction = new Action("Refresh") {
			public void run() {
				Object[] expandedElements = treeViewer.getExpandedElements();
				treeViewer.setInput(NavigatorContentProvider.ROOT_GROUP);
				treeViewer.setExpandedElements(expandedElements);
			}
		};
		refreshAction.setToolTipText("Refresh the navigator");
		refreshAction.setImageDescriptor(IconManager.getIconDescriptor("refresh.png"));
		mgr.add(refreshAction);
	}

	private void hookDoubleClickAction() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				TreeSelection sel = (TreeSelection) event.getSelection();
				Object el = sel.getFirstElement();
				IElement e = (IElement)el;
				if (e instanceof IGroup) {
					treeViewer.setExpandedState(el, !treeViewer.getExpandedState(el));
				} else {
					ElementHandlerRegistry.getInstance().handleDoubleClick(e);
				}
			}
		});
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		TreeSelection sel = (TreeSelection)treeViewer.getSelection();
		IElement[] elements = new IElement[sel.size()];
		SelectionUtils.getObjects(sel, IElement.class).toArray(elements);
		ElementHandlerRegistry.getInstance().createContextMenu(elements, manager);
	}

	private void initListeners() {
		eventListener = new IModelEventListener() {
			@Override
			public void handleEvent(ModelEvent event) {
				if (event.source != null &&
						(event.type == ModelEventType.ObjectCreated
						|| event.type == ModelEventType.ObjectChanged
						|| event.type == ModelEventType.ObjectRemoved)) {

					NavigatorContentProvider provider = (NavigatorContentProvider)treeViewer.getContentProvider();
					boolean providesClass = provider.providesElementClass(event.source.getClass());

					if (providesClass) {
						refreshTree(event);
					} else {
						// This event has nothing to do with the objects shown in the tree. Ignore it.
					}
				}
			}
		};
		ModelEventService.getInstance().addEventListener(eventListener);
	}

	private class RefreshTreeCallback implements Runnable {
		private ModelEvent event;

		private RefreshTreeCallback(ModelEvent event) {
			this.event = event;
		}

		@Override
		public void run() {
			if (treeViewer.getControl().isDisposed()) {
				return;
			}

			treeViewer.refresh();
			if (event != null && event.type != ModelEventType.ObjectRemoved) {
				IElement selectedElement = findElement(event.source, (IElement) treeViewer.getInput());
				if (selectedElement != null) {
					treeViewer.reveal(selectedElement);
					treeViewer.setSelection(new StructuredSelection(selectedElement));
				}
			}
		}

		private IElement findElement(Object source, IElement parentElement) {
			Object[] children = ((ITreeContentProvider) treeViewer.getContentProvider()).getChildren(parentElement);
			for (Object child : children) {
				if (child instanceof Group) {
					IElement result = findElement(source, (Group) child);
					if (result != null) {
						return result;
					}
				} else if (child instanceof Element) {
					if (source.equals(((Element) child).getData())) {
						return (Element) child;
					}
				}
			}
			return null;
		}
	}
}
