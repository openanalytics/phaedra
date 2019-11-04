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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
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

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.cmd.BrowseProtocols;
import eu.openanalytics.phaedra.ui.protocol.table.ProtocolClassTableColumns;
import eu.openanalytics.phaedra.ui.protocol.util.ProtocolClassSummaryLoader;

public class ProtocolClassBrowser extends EditorPart {

	private RichTableViewer tableViewer;

	private List<ProtocolClass> protocolClasses;
	private ProtocolClassSummaryLoader summaryLoader;

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
		tableViewer = new RichTableViewer(parent, SWT.VIRTUAL, getClass().getSimpleName(), true);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(ProtocolClassTableColumns.configureColumns(summaryLoader));
		tableViewer.setDefaultSearchColumn("Protocol Class Name");
		tableViewer.setInput(protocolClasses);
		getSite().setSelectionProvider(tableViewer);

		hookDoubleClickAction();
		createContextMenu();
		initEventListener();

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent
				, "eu.openanalytics.phaedra.ui.help.viewProtocolClassBrowser");
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		summaryLoader.stop();
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
		VOEditorInput input = (VOEditorInput)getEditorInput();
		List<IValueObject> valueObjects = input.getValueObjects();
		
		// Make a copy of the list and sort it.
		protocolClasses = new ArrayList<ProtocolClass>();
		for (IValueObject vo: valueObjects) protocolClasses.add((ProtocolClass) vo);
		Collections.sort(protocolClasses, ProtocolUtils.PROTOCOLCLASS_NAME_SORTER);

		// Pre-fetch summaries.
		summaryLoader = new ProtocolClassSummaryLoader(protocolClasses, pClass -> {
			if (tableViewer != null && !tableViewer.getControl().isDisposed()) tableViewer.refresh(pClass);
		});
		summaryLoader.start();
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
				if (src instanceof ProtocolClass) {
					loadTableData();
					Display.getDefault().asyncExec(refreshTableCallback);
				}
			}
		};
		ModelEventService.getInstance().addEventListener(eventListener);
	}

	private Runnable refreshTableCallback = new Runnable(){
		@Override
		public void run() {
			tableViewer.setInput(protocolClasses);
		}
	};

	private void hookDoubleClickAction() {
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				Object item = sel.getFirstElement();
				if (item instanceof ProtocolClass) {
					IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(IHandlerService.class);
					try {
						handlerService.executeCommand(BrowseProtocols.class.getName(), null);
					} catch (Exception e) {
						MessageDialog.openError(Display.getDefault().getActiveShell(),
								"Failed to open Protocol Browser", "Failed to open Protocol Browser: " + e.getMessage());
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