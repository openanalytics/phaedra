package eu.openanalytics.phaedra.ui.plate.browser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.util.misc.SelectionProviderIntermediate;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.util.MultiplateWellAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.table.MultiplateWellTableColumns;

public class MultiplateWellBrowser extends EditorPart {

	private CTabFolder tabFolder;
	private CTabItem tableTab;

	private RichTableViewer tableViewer;

	private MultiplateWellAccessor wellAccessor;
	private List<PlateDataAccessor> dataAccessors;

	private SelectionProviderIntermediate selectionProvider;
	private ISelectionListener selectionListener;
	private IModelEventListener modelEventListener;

	private boolean tableTabInitialized;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	public void createPartControl(Composite parent) {

		List<Plate> plates = getPlates();

		wellAccessor = new MultiplateWellAccessor(plates);
		dataAccessors = new ArrayList<PlateDataAccessor>();
		for (Plate plate : plates) dataAccessors.add(CalculationService.getInstance().getAccessor(plate));

		tabFolder = new CTabFolder(parent, SWT.BOTTOM | SWT.V_SCROLL | SWT.H_SCROLL);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
		tabFolder.addListener(SWT.Selection, e -> tabChanged(e.item));

		/* Table tab */

		tableTab = new CTabItem(tabFolder, SWT.NONE);
		tableTab.setText("Multi-Plate Table View");

		Composite container = new Composite(tabFolder, SWT.NONE);
		container = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		tableTab.setControl(container);

		tableViewer = new RichTableViewer(container, SWT.NONE, getClass().getSimpleName());
		tableViewer.setContentProvider(new ArrayContentProvider());
		GridDataFactory.fillDefaults().grab(true,true).applyTo(tableViewer.getControl());

		/* Other */

		createContextMenu();

		selectionProvider = new SelectionProviderIntermediate();
		selectionProvider.addSelectionChangedListener(event -> MultiplateWellBrowser.this.selectionChanged(event));
		selectionProvider.setSelectionProviderDelegate(tableViewer);
		getSite().setSelectionProvider(selectionProvider);

		selectionListener = (part, selection) -> {
			if (part == MultiplateWellBrowser.this) return;
			List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
			if (wells.isEmpty()) {
				List<Compound> compounds = SelectionUtils.getObjects(selection, Compound.class);
				if (!compounds.isEmpty()) {
					wells = new ArrayList<Well>();
					for (Compound c: compounds) wells.addAll(c.getWells());
				}
			}
			if (!wells.isEmpty()) {
				tableViewer.setSelection(new StructuredSelection(wells));
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		modelEventListener = event -> {
			if (event.type == ModelEventType.Calculated) {
				if (event.source instanceof Plate) {
					Plate plate = (Plate)event.source;
					List<Plate> currentPlates = getPlates();
					for(Plate currentPlate : currentPlates)
						if (currentPlate.equals(plate)) {
							Display.getDefault().asyncExec(refreshUICallback);
					}
				}
			}
		};
		ModelEventService.getInstance().addEventListener(modelEventListener);

		tabFolder.setSelection(tableTab);
		if (!tableTabInitialized) initializeTableTab();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ModelEventService.getInstance().removeEventListener(modelEventListener);
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

	private Runnable refreshUICallback = () -> tableViewer.setInput(wellAccessor.getWells());

	private List<Plate> getPlates() {
		VOEditorInput input = (VOEditorInput)getEditorInput();
		List<IValueObject> valueObjects = input.getValueObjects();
		List<Plate> plates = new ArrayList<>();
		for (IValueObject vo: valueObjects) if (vo instanceof Plate) plates.add((Plate)vo);
		return plates;
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));

		// The context menu for the table viewer.
		menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, tableViewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		// Contributions are added here.
		manager.add(new Separator());
		tableViewer.contributeConfigButton(manager);
	}

	private void tabChanged(Widget tab) {
		// Update the selection provider intermediate.
		if (tab == tableTab) {
			selectionProvider.setSelectionProviderDelegate(tableViewer);
			if (!tableTabInitialized) initializeTableTab();
		}
	}

	private void initializeTableTab() {
		Job loadDataJob = new Job("Loading Data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading Data", IProgressMonitor.UNKNOWN);
				dataAccessors.stream().forEach(da -> da.loadEager(null));

				tableTabInitialized = true;

				Display.getDefault().syncExec(() -> {
					if (!tableViewer.getTable().isDisposed()) {
						tableViewer.applyColumnConfig(MultiplateWellTableColumns.configureColumns(dataAccessors.get(0), tableViewer));
						tableViewer.setInput(wellAccessor.getWells());
					}
				});

				return Status.OK_STATUS;
			};
		};
		loadDataJob.setUser(true);
		loadDataJob.schedule();
	}

	private void selectionChanged(SelectionChangedEvent event) {
		//we have only one tab, no action required here
	}

	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

}
