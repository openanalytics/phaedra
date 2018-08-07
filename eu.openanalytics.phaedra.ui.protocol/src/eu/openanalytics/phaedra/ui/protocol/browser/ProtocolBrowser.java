package eu.openanalytics.phaedra.ui.protocol.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.util.misc.DNDSupport;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.ui.protocol.table.ProtocolTableColumns;

public class ProtocolBrowser extends EditorPart {

	private BreadcrumbViewer breadcrumb;
	private RichTableViewer tableViewer;

	private boolean isProtocolClassInput;
	private List<Protocol> protocols;

	private IModelEventListener eventListener;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());

		loadTableData();
	}

	@Override
	public void createPartControl(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(container);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(container);
		if (isProtocolClassInput && !protocols.isEmpty()) breadcrumb.setInput(protocols.get(0).getProtocolClass());
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 23).applyTo(breadcrumb.getControl());

		tableViewer = new RichTableViewer(container, SWT.NONE, getClass().getSimpleName(), true);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(ProtocolTableColumns.configureColumns());
		tableViewer.setDefaultSearchColumn("Protocol Name");
		tableViewer.setInput(protocols);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Protocol prot = SelectionUtils.getFirstObject(event.getSelection(), Protocol.class);
				if (prot != null) {
					breadcrumb.setInput(prot.getProtocolClass());
					breadcrumb.getControl().getParent().layout();
				}
			}
		});
		// Because TableViewer was created with a toolbar it is actually contained within 2 other composites.
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getControl().getParent().getParent());
		getSite().setSelectionProvider(tableViewer);

		DNDSupport.addDragSupport(tableViewer, this);
		hookDoubleClickAction();
		createContextMenu();
		initEventListener();

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent
				, "eu.openanalytics.phaedra.ui.help.viewProtocolBrowser");
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		ModelEventService.getInstance().removeEventListener(eventListener);
		super.dispose();
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Do nothing.
	}

	@Override
	public void doSaveAs() {
		// Do nothing.
	}

	private void loadTableData() {
		// Make a copy of the list and sort it.
		VOEditorInput input = (VOEditorInput)getEditorInput();
		List<IValueObject> valueObjects = input.getValueObjects();
		protocols = new ArrayList<>();
		if (!valueObjects.isEmpty()) {
			if (valueObjects.get(0) instanceof ProtocolClass) {
				for (IValueObject vo: valueObjects) protocols.addAll(ProtocolService.getInstance().getProtocols((ProtocolClass)vo));
				isProtocolClassInput = true;
			} else if (valueObjects.get(0) instanceof Protocol) {
				for (IValueObject vo: valueObjects) protocols.add((Protocol)vo);
			}
		}
		Collections.sort(protocols, ProtocolUtils.PROTOCOL_NAME_SORTER);
	}

	private void initEventListener() {
		eventListener = new IModelEventListener() {
			@Override
			public void handleEvent(ModelEvent event) {
				boolean structChanged = event.type == ModelEventType.ObjectCreated
						|| event.type == ModelEventType.ObjectChanged
						|| event.type == ModelEventType.ObjectRemoved;
				if (!structChanged) return;

				Object src = event.source;
				if (src instanceof Protocol || src instanceof ProtocolClass) {
					//TODO Don't load all protocols, depends on editor input.
					protocols = new ArrayList<Protocol>(ProtocolService.getInstance().getProtocols());
					Collections.sort(protocols, ProtocolUtils.PROTOCOL_NAME_SORTER);
					Display.getDefault().asyncExec(refreshTableCallback);
				}
			}
		};
		ModelEventService.getInstance().addEventListener(eventListener);
	}

	private Runnable refreshTableCallback = new Runnable(){
		@Override
		public void run() {
			tableViewer.setInput(protocols);
		}
	};

	private void hookDoubleClickAction() {
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				Object item = sel.getFirstElement();
				if (item instanceof Protocol) {
					IHandlerService handlerService = (IHandlerService)getSite().getService(IHandlerService.class);
					try {
						//TODO Get rid of hardcoded command id.
						handlerService.executeCommand("eu.openanalytics.phaedra.ui.plate.cmd.BrowseExperiments", null);
					} catch (Exception e) {
						MessageDialog.openError(Display.getDefault().getActiveShell(),
								"Failed to open Experiment Browser", "Failed to open Experiment Browser: " + e.getMessage());
					}
				}
			}
		});
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, tableViewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		tableViewer.contributeConfigButton(manager);
	}

}