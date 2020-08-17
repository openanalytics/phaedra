package eu.openanalytics.phaedra.ui.protocol.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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

import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.misc.WorkbenchSiteJobScheduler;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataDirectViewerInput;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataViewerInput;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.cmd.BrowseProtocols;
import eu.openanalytics.phaedra.ui.protocol.table.ProtocolClassTableColumns;
import eu.openanalytics.phaedra.ui.protocol.util.ProtocolClasses;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.DynamicColumnSupport;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.EvaluationContext;


public class ProtocolClassBrowser extends EditorPart {
	
	
	private AsyncDataViewerInput<ProtocolClass, ProtocolClass> viewerInput;
	private ProtocolClasses<ProtocolClass> protocolClasses;
	private AsyncDataLoader<ProtocolClass> dataLoader;
	
	private EvaluationContext<ProtocolClass> evaluationContext;
	
	private DataFormatSupport dataFormatSupport;
	
	private RichTableViewer tableViewer;


	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}
	
	private void initViewerInput() {
		this.dataLoader = new AsyncDataLoader<>("data for protocol class browser",
				new WorkbenchSiteJobScheduler(this) );
		this.viewerInput = new AsyncDataDirectViewerInput<ProtocolClass>(ProtocolClass.class, this.dataLoader) {
			
			@Override
			protected List<ProtocolClass> loadElements() {
				VOEditorInput input = (VOEditorInput)getEditorInput();
				List<IValueObject> valueObjects = input.getValueObjects();
				List<ProtocolClass> protocolClasses = new ArrayList<ProtocolClass>();
				for (IValueObject vo: valueObjects) protocolClasses.add((ProtocolClass)vo);
				Collections.sort(protocolClasses, ProtocolUtils.PROTOCOLCLASS_NAME_SORTER);
				return protocolClasses;
			}
			
		};
		this.protocolClasses = new ProtocolClasses<>(this.viewerInput, Function.identity());
		
		this.evaluationContext = new EvaluationContext<>(this.viewerInput, this.protocolClasses);
	}
	
	
	@Override
	public void createPartControl(Composite parent) {
		initViewerInput();
		this.dataFormatSupport = new DataFormatSupport(this.viewerInput::refreshViewer);
		
		final DynamicColumnSupport<ProtocolClass, ProtocolClass> customColumnSupport = new DynamicColumnSupport<>(
				this.viewerInput, this.evaluationContext, this.dataFormatSupport );
		
		tableViewer = new RichTableViewer(parent, SWT.VIRTUAL, getClass().getSimpleName(),
				customColumnSupport, true );
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(ProtocolClassTableColumns.configureColumns(this.dataLoader));
		tableViewer.setDefaultSearchColumn("Protocol Class Name");
		
		this.viewerInput.connect(tableViewer);
		getSite().setSelectionProvider(tableViewer);

		hookDoubleClickAction();
		createContextMenu();

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
				"eu.openanalytics.phaedra.ui.help.viewProtocolClassBrowser");
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		if (this.viewerInput != null) this.viewerInput.dispose();
		if (this.dataLoader != null) this.dataLoader.dispose();
		if (this.evaluationContext != null) this.evaluationContext.dispose();
		if (this.dataFormatSupport != null) this.dataFormatSupport.dispose();
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
	
	private void refresh() {
		this.tableViewer.refresh();
	}

	private void hookDoubleClickAction() {
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				Object item = sel.getFirstElement();
				if (item instanceof ProtocolClass) {
					IHandlerService handlerService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(IHandlerService.class);
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
