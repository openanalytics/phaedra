package eu.openanalytics.phaedra.ui.plate.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.misc.DNDSupport;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataDirectViewerInput;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataViewerInput;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.plate.cmd.BrowsePlates;
import eu.openanalytics.phaedra.ui.plate.table.ExperimentTableColumns;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;

public class ExperimentBrowser extends EditorPart {
	
	
	private AsyncDataViewerInput<Experiment, Experiment> viewerInput;
	private AsyncDataLoader<Experiment> dataLoader;
	
	private BreadcrumbViewer breadcrumb;
	private RichTableViewer tableViewer;


	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		
		this.dataLoader = new AsyncDataLoader<>("data for experiment browser");
		this.viewerInput = new AsyncDataDirectViewerInput<Experiment>(Experiment.class, this.dataLoader) {
			
			@Override
			protected List<Experiment> loadElements() {
				VOEditorInput input = (VOEditorInput)getEditorInput();
				List<IValueObject> valueObjects = input.getValueObjects();
				List<Experiment> experiments = new ArrayList<>();
				if (!valueObjects.isEmpty()) {
					if (valueObjects.get(0) instanceof Protocol) {
						for (IValueObject vo: valueObjects) experiments.addAll(PlateService.getInstance().getExperiments((Protocol)vo));
					} else if (valueObjects.get(0) instanceof Experiment) {
						for (IValueObject vo: valueObjects) experiments.add((Experiment)vo);
					}
				}
				Collections.sort(experiments, PlateUtils.EXPERIMENT_NAME_SORTER);
				return experiments;
			}
			
			@Override
			protected void checkChangedElements(final Object[] eventElements, final Consumer<Experiment> task) {
				for (final Object o : eventElements) {
					final Experiment experiment = SelectionUtils.getAsClass(o, Experiment.class);
					if (experiment != null) {
						task.accept(experiment);
					}
				}
			}
			
		};
	}
	
	
	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(container);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(container);
		List<Experiment> experiments = viewerInput.getBaseElements();
		breadcrumb.setInput((experiments != null && !experiments.isEmpty()) ? experiments.get(0).getProtocol() : null);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		tableViewer = new RichTableViewer(container, SWT.NONE, getClass().getSimpleName(), true);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(ExperimentTableColumns.configureColumns(this.dataLoader));
		tableViewer.setDefaultSearchColumn("Name");
		// Because TableViewer was created with a toolbar it is actually contained within 2 other composites.
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getControl().getParent().getParent());
		
		this.viewerInput.connect(tableViewer);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Experiment exp = SelectionUtils.getFirstObject(event.getSelection(), Experiment.class);
				if (exp != null) {
					breadcrumb.setInput(exp.getProtocol());
				}
			}
		});

		DNDSupport.addDragSupport(tableViewer, this);
		getSite().setSelectionProvider(tableViewer);

		hookDoubleClickAction();
		createContextMenu();

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
				"eu.openanalytics.phaedra.ui.help.viewExperimentBrowser");
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		if (this.viewerInput != null) this.viewerInput.dispose();
		if (this.dataLoader != null) this.dataLoader.dispose();
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
	
	private void hookDoubleClickAction() {
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				Object item = sel.getFirstElement();
				if (item instanceof Experiment) {
					IHandlerService handlerService = getSite().getService(IHandlerService.class);
					try {
						handlerService.executeCommand(BrowsePlates.class.getName(), null);
					} catch (Exception e) {
						MessageDialog.openError(Display.getDefault().getActiveShell(),
								"Failed to open Plate Browser", "Failed to open Plate Browser: " + e.getMessage());
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
