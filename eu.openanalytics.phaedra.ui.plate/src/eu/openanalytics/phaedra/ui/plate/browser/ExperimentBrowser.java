package eu.openanalytics.phaedra.ui.plate.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.ExperimentSummary;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.plate.cmd.BrowsePlates;
import eu.openanalytics.phaedra.ui.plate.table.ExperimentTableColumns;
import eu.openanalytics.phaedra.ui.plate.util.ExperimentSummaryLoader;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;

public class ExperimentBrowser extends EditorPart {

	private BreadcrumbViewer breadcrumb;
	private RichTableViewer tableViewer;

	private ExperimentSummaryLoader summaryLoader;

	private List<Experiment> experiments;
	private IModelEventListener eventListener;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());

		initEventListener();
		loadData();
	}

	@Override
	public void createPartControl(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(container);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(container);
		if (experiments != null && !experiments.isEmpty()) {
			breadcrumb.setInput(experiments.get(0).getProtocol());
		}
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		tableViewer = new RichTableViewer(container, SWT.NONE, getClass().getSimpleName(), true);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(ExperimentTableColumns.configureColumns(summaryLoader));
		tableViewer.setDefaultSearchColumn("Name");
		tableViewer.setInput(experiments);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Experiment exp = SelectionUtils.getFirstObject(event.getSelection(), Experiment.class);
				if (exp != null) {
					breadcrumb.setInput(exp.getProtocol());
				}
			}
		});
		// Because TableViewer was created with a toolbar it is actually contained within 2 other composites.
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getControl().getParent().getParent());

		DNDSupport.addDragSupport(tableViewer, this);
		getSite().setSelectionProvider(tableViewer);

		hookDoubleClickAction();
		createContextMenu();

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewExperimentBrowser");

		summaryLoader.start();
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

	private List<Experiment> getExperiments() {
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
		return experiments;
	}

	private void loadData() {
		// Make a copy of the list and sort it.
		experiments = getExperiments();
		Collections.sort(experiments, PlateUtils.EXPERIMENT_NAME_SORTER);

		// Pre-fetch summaries.
		summaryLoader = new ExperimentSummaryLoader(experiments, exp -> {
			if (tableViewer != null && !tableViewer.getControl().isDisposed()) tableViewer.refresh(exp);
		});
		for (Experiment exp: experiments) {
			ExperimentSummary summary = new ExperimentSummary();
			List<Plate> plates = PlateService.getInstance().getPlates(exp);
			summary.plates = plates.size();
			summary.platesToCalculate = (int) plates.stream().filter(PlateUtils.CALCULATION_TODO).count();
			summary.platesToValidate = (int) plates.stream().filter(PlateUtils.VALIDATION_TODO).count();
			summary.platesToApprove = (int) plates.stream().filter(PlateUtils.APPROVAL_TODO).count();
			summary.platesToExport = (int) plates.stream().filter(PlateUtils.EXPORT_TODO).count();
			summaryLoader.update(exp, summary);
		}
	}

	private void initEventListener() {
		eventListener = new IModelEventListener() {
			@Override
			public void handleEvent(ModelEvent event) {
				boolean structChanged = event.type == ModelEventType.ObjectCreated
						|| event.type == ModelEventType.ObjectChanged
						|| event.type == ModelEventType.ObjectRemoved;
				if (!structChanged) return;

				Object[] items = ModelEventService.getEventItems(event);

				List<Experiment> reloadedExperiments = getExperiments();
				Collections.sort(reloadedExperiments, PlateUtils.EXPERIMENT_NAME_SORTER);
				Set<Experiment> changedExperiments = new HashSet<>();

				for (Object o: items) {
					Experiment exp = SelectionUtils.getAsClass(o, Experiment.class);
					if (exp == null) continue;
					boolean wasInList = experiments.contains(exp);
					boolean isInList = reloadedExperiments.contains(exp);
					if (isInList || (wasInList && !isInList)) changedExperiments.add(exp);
				}

				for (Experiment exp: changedExperiments) summaryLoader.update(exp);
				if (!changedExperiments.isEmpty()) {
					experiments = reloadedExperiments;
					Display.getDefault().asyncExec(() -> tableViewer.setInput(experiments));
				}
			}
		};
		ModelEventService.getInstance().addEventListener(eventListener);
	}

	private void hookDoubleClickAction() {
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				Object item = sel.getFirstElement();
				if (item instanceof Experiment) {
					IHandlerService handlerService = (IHandlerService)getSite().getService(IHandlerService.class);
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
