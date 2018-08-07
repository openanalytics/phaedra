package eu.openanalytics.phaedra.ui.plate.browser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewerUtils;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.MultiGridLayerSupport;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.misc.DNDSupport;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedEditor;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionProviderIntermediate;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.partsettings.utils.PartSettingsUtils;
import eu.openanalytics.phaedra.ui.plate.cmd.BrowseWells;
import eu.openanalytics.phaedra.ui.plate.grid.PlateContentProvider;
import eu.openanalytics.phaedra.ui.plate.grid.layer.HeatmapLayer;
import eu.openanalytics.phaedra.ui.plate.grid.layer.MultiFeatureLayer;
import eu.openanalytics.phaedra.ui.plate.table.PlateTableColumns;
import eu.openanalytics.phaedra.ui.plate.util.PlateGrid;
import eu.openanalytics.phaedra.ui.plate.util.PlateSummaryLoader;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class PlateBrowser extends DecoratedEditor {

	public static final String PLATEGRID_SETTINGS = "SETTINGS_PLATEGRID";

	private BreadcrumbViewer breadcrumb;
	private CTabFolder tabFolder;

	private SelectionProviderIntermediate selectionProvider;
	private IUIEventListener featureSelectionListener;
	private IModelEventListener eventListener;

	private Experiment singleExperiment;
	private List<Plate> plates;

	/* Tab 1: table */
	private CTabItem tableTab;
	private RichTableViewer tableViewer;
	private PlateSummaryLoader summaryLoader;

	/* Tab 2: plate heatmaps */
	private CTabItem thumbnailsTab;
	private PlateGrid plateGrid;
	private MultiGridLayerSupport gridLayerSupport;
	private LoadFeatureDataJob loadDataJob;
	private boolean thumbsLoaded;

	/* Tab 3: feature heatmaps */
	private CTabItem featureHeatmapsTab;
	private PlateFeatureHeatmapsTab featureHeatmapsTabComposite;
	private boolean featureHeatmapsLoaded;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());

		loadPlates();
		loadDataJob = new LoadFeatureDataJob();
		summaryLoader = new PlateSummaryLoader(plates, plate -> {
			if (tableViewer != null && !tableViewer.getControl().isDisposed()) tableViewer.refresh(plate);
		});
	}

	@Override
	public void createPartControl(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(container);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(container);
		breadcrumb.setInput(plates != null && !plates.isEmpty() ? plates.get(0).getExperiment() : null);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		tabFolder = new CTabFolder(container, SWT.BOTTOM | SWT.V_SCROLL | SWT.H_SCROLL);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
		tabFolder.addListener(SWT.Selection, e -> {
			Widget w = e.item;
			tabChanged(w);
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tabFolder);

		/* Plate List Tab */

		tableTab = new CTabItem(tabFolder, SWT.NONE);
		tableTab.setText("Plate List");

		container = new Composite(tabFolder, SWT.NONE);
		container.setLayout(new FillLayout());
		tableTab.setControl(container);

		tableViewer = new RichTableViewer(container, SWT.NONE, getClass().getSimpleName(), true);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(PlateTableColumns.configureColumns(false, summaryLoader));
		tableViewer.setDefaultSearchColumn("Barcode");
		tableViewer.setInput(plates);

		/* Plate Thumbnails Tab */

		thumbnailsTab = new CTabItem(tabFolder, SWT.NONE);
		thumbnailsTab.setText("Plate Heatmaps");

		plateGrid = new PlateGrid(tabFolder, SWT.NONE);
		plateGrid.addSorter("Sequence", (o1, o2) -> {
			Plate p1 = (Plate)o1.getInput();
			Plate p2 = (Plate)o2.getInput();
			if (p1 == null && p2 == null) return 0;
			if (p1 == null) return -1;
			if (p2 == null) return 1;
			return p1.getSequence() - p2.getSequence();
		});
		plateGrid.addSorter("Barcode", (o1, o2) -> {
			Plate p1 = (Plate)o1.getInput();
			Plate p2 = (Plate)o2.getInput();
			if (p1 == null && p2 == null) return 0;
			if (p1 == null) return -1;
			if (p2 == null) return 1;
			return p1.getBarcode().compareTo(p2.getBarcode());
		});
		plateGrid.addSorter("Validation", (o1, o2) -> {
			Plate p1 = (Plate)o1.getInput();
			Plate p2 = (Plate)o2.getInput();
			if (p1 == null && p2 == null) return 0;
			if (p1 == null) return -1;
			if (p2 == null) return 1;
			return p1.getValidationStatus() - p2.getValidationStatus();
		});
		thumbnailsTab.setControl(plateGrid);

		int plateCount = plates.size();
		for (int i=0; i<plateCount; i++) {
			final Plate plate = plates.get(i);
			final GridViewer viewer = plateGrid.createGridViewer(plate);
			viewer.setContentProvider(new PlateContentProvider());
			viewer.getGrid().addListener(SWT.MouseDoubleClick, e -> openWellBrowser(new StructuredSelection(plate)));

			if (i == 0) {
				gridLayerSupport = new MultiGridLayerSupport("hca.singlewell.grid|hca.well.grid", viewer);
				gridLayerSupport.setAttribute("featureProvider", ProtocolUIService.getInstance());
				gridLayerSupport.setAttribute(GridLayerSupport.IS_HIDDEN, getEditorSite().getActionBars().getServiceLocator() == null);
				viewer.setLabelProvider(gridLayerSupport.createLabelProvider());
			} else {
				gridLayerSupport.linkViewer(viewer);
			}
		}

		/* Feature heatmaps tab */

		featureHeatmapsTab = new CTabItem(tabFolder, SWT.NONE);
		featureHeatmapsTab.setText("Feature Heatmaps");
		featureHeatmapsTabComposite = new PlateFeatureHeatmapsTab(tabFolder, SWT.NONE, plates);
		featureHeatmapsTab.setControl(featureHeatmapsTabComposite);

		/* Other behavior */

		initEventListener();
		initSelectionListener();

		DNDSupport.addDragSupport(tableViewer, this);
		DNDSupport.addDropSupport(tableViewer, this, e -> singleExperiment);

		hookDoubleClickAction();
		createContextMenu();
		for (int i=0; i<plates.size(); i++) createThumbContextMenu(i);
		getSite().registerContextMenu(featureHeatmapsTabComposite.getMenuMgr(), featureHeatmapsTabComposite.getSelectionProvider());

		tabFolder.setSelection(tableTab);
		selectionProvider = new SelectionProviderIntermediate();
		selectionProvider.setSelectionProviderDelegate(tableViewer);
		selectionProvider.addSelectionChangedListener(event -> {
			Object src = event.getSource();
			ISelection selection = new StructuredSelection(SelectionUtils.getObjects(event.getSelection(), Plate.class));
			Plate plate = SelectionUtils.getFirstObject(event.getSelection(), Plate.class);
			ISelection singleSelection = (plate == null) ? null : new StructuredSelection(plate);

			// Sync selections between tabs.
			if (src == tableViewer) {
				if (singleSelection != null) plateGrid.setSelection(singleSelection);
				featureHeatmapsTabComposite.getSelectionProvider().setSelection(selection);
			} else if (src == plateGrid) {
				featureHeatmapsTabComposite.getSelectionProvider().setSelection(selection);
				tableViewer.setSelection(selection);
			} else if (src == featureHeatmapsTabComposite.getSelectionProvider()) {
				if (singleSelection != null) plateGrid.setSelection(singleSelection);
				tableViewer.setSelection(selection);
			}
		});
		getSite().setSelectionProvider(selectionProvider);

		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		addDecorator(new CopyableDecorator());
		initDecorators(parent, plateGrid);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewPlateBrowser");

		summaryLoader.start();
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		if (plateGrid != null) plateGrid.dispose();
		if (gridLayerSupport != null) gridLayerSupport.dispose();
		if (loadDataJob != null) loadDataJob.cancel();
		ModelEventService.getInstance().removeEventListener(eventListener);
		ProtocolUIService.getInstance().removeUIEventListener(featureSelectionListener);
		featureHeatmapsTabComposite.dispose();
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

	@Override
	public void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator("plateMenu"));
		tableViewer.contributeConfigButton(manager);
		super.fillContextMenu(manager);
	}
	
	@Override
	protected IToolBarManager getToolBarManager() {
		return plateGrid.getToolBarManager();
	}

	private void tabChanged(Widget tab) {
		if (tab == tableTab) {
			selectionProvider.setSelectionProviderDelegate(tableViewer);
		} else if (tab == thumbnailsTab) {
			selectionProvider.setSelectionProviderDelegate(plateGrid);
			if (!thumbsLoaded) {
				thumbsLoaded = true;
				loadDataJob.schedule();
			}
		} else if (tab == featureHeatmapsTab) {
			selectionProvider.setSelectionProviderDelegate(featureHeatmapsTabComposite.getSelectionProvider());
			if (!featureHeatmapsLoaded) {
				featureHeatmapsLoaded = true;
				featureHeatmapsTabComposite.startLoading();
			}
		}
	}

	private void initEventListener() {
		eventListener = event -> {
			boolean structChanged = event.type == ModelEventType.ObjectCreated
					|| event.type == ModelEventType.ObjectChanged
					|| event.type == ModelEventType.ObjectRemoved;
			if (!structChanged) return;

			boolean refreshTable = false;
			Set<Plate> changedPlates = new HashSet<>();
			Object[] items = ModelEventService.getEventItems(event);
			for (Object src: items) {
				if (src instanceof Plate) {
					Plate plate = (Plate)src;
					VOEditorInput input = (VOEditorInput)getEditorInput();
					Experiment experiment = SelectionUtils.getFirstAsClass(input.getValueObjects(), Experiment.class);
					if (plate.getExperiment().equals(experiment)) {
						changedPlates.add(plate);
						refreshTable = true;
					} else if (plates.contains(plate)) {
						// Listed but not same parent: plate moved to another exp.
						refreshTable = true;
					}
				} else if (src.equals(singleExperiment)) {
					Display.getDefault().asyncExec(() -> {
						setPartName(singleExperiment.getName());
						breadcrumb.refresh();
					});
				}
			}

			for (Plate plate: changedPlates) summaryLoader.update(plate);
			if (refreshTable) {
				loadPlates();
				Display.getDefault().asyncExec(refreshTableCallback);
			}
		};
		ModelEventService.getInstance().addEventListener(eventListener);
	}

	private Runnable refreshTableCallback = () -> {
		if (tableViewer != null && tableViewer.getTable() != null && !tableViewer.getTable().isDisposed()) {
			tableViewer.setInput(plates);
		}
		if (plateGrid != null && !plateGrid.isDisposed()) {
			plateGrid.refreshHeaders();
		}
	};

	private void initSelectionListener() {
		featureSelectionListener = event -> {
			if (tableViewer.getTable() == null || tableViewer.getTable().isDisposed()) return;

			if (event.type == EventType.FeatureSelectionChanged || event.type == EventType.NormalizationSelectionChanged
					|| event.type == EventType.ColorMethodChanged) {
				ProtocolClass pClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
				for (Plate plate: plates) {
					ProtocolClass thisPClass = PlateUtils.getProtocolClass(plate);
					if (thisPClass.equals(pClass)) {
						tableViewer.setInput(plates);
						loadDataJob.schedule();
						return;
					}
				}
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(featureSelectionListener);
	}

	private void hookDoubleClickAction() {
		tableViewer.addDoubleClickListener(event -> openWellBrowser(event.getSelection()));
	}

	private void openWellBrowser(ISelection selection) {
		Object item = ((StructuredSelection)selection).getFirstElement();
		if (item instanceof Plate) {
			IHandlerService handlerService = (IHandlerService)getSite().getService(IHandlerService.class);
			try {
				handlerService.executeCommand(BrowseWells.class.getName(), null);
			} catch (Exception e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						"Failed to open Well Browser", "Failed to open Well Browser: " + e.getMessage());
			}
		}
	}

	/* Context menus */

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, tableViewer);
	}

	private void createThumbContextMenu(final int plateIndex) {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillThumbContextMenu(plateIndex, manager));
		Menu menu = menuMgr.createContextMenu(plateGrid.getGridViewer(plateIndex).getControl());
		plateGrid.getGridViewer(plateIndex).getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, plateGrid.getGridViewer(plateIndex));
	}

	private void fillThumbContextMenu(int plateIndex, IMenuManager manager) {
		manager.add(new Separator("wellMenu"));
		// Contributions are added here.
		manager.add(new Separator());
		gridLayerSupport.contributeContextMenu(manager);
	}

	/* Plate loading */

	private void loadPlates() {
		VOEditorInput input = (VOEditorInput)getEditorInput();
		List<IValueObject> valueObjects = input.getValueObjects();
		plates = new ArrayList<>();
		if (!valueObjects.isEmpty()) {
			if (valueObjects.get(0) instanceof Experiment) {
				for (IValueObject vo: valueObjects) plates.addAll(PlateService.getInstance().getPlates((Experiment)vo));
			} else if (valueObjects.get(0) instanceof Plate) {
				for (IValueObject vo: valueObjects) plates.add((Plate)vo);
			}
		}
		if (valueObjects.size() == 1 && valueObjects.get(0) instanceof Experiment) {
			singleExperiment = (Experiment) valueObjects.get(0);
			// Set the current protocolclass, when this editor is opened directly (for example via My Favorites).
			Display.getDefault().asyncExec(() -> ProtocolUIService.getInstance().setCurrentProtocolClass(singleExperiment.getProtocol().getProtocolClass()));
		}
	}

	private boolean hasFeatureLayer() {
		return GridViewerUtils.hasGridLayerEnabled(gridLayerSupport, HeatmapLayer.class, MultiFeatureLayer.class);
	}

	private Protocol getProtocol() {
		return singleExperiment.getProtocol();
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		for (IGridLayer layer : gridLayerSupport.getLayers()) {
			if (layer.isEnabled()) {
				Object layerConfig = layer.getConfig();
				if (layerConfig == null) layerConfig = true;
				properties.addProperty(layer.getName() + GridViewerUtils.CONFIG, layerConfig);
			}
		}
		properties.addProperty(PLATEGRID_SETTINGS, plateGrid.getConfig());
		if (hasFeatureLayer()) {
			Feature feature = ProtocolUIService.getInstance().getCurrentFeature();
			String norm = ProtocolUIService.getInstance().getCurrentNormalization();
			PartSettingsUtils.setFeature(properties, feature);
			PartSettingsUtils.setNormalization(properties, norm);
		}
		return properties;
	}

	private void setProperties(Properties properties) {
		HashMap<String, Object> config = properties.getProperty(PLATEGRID_SETTINGS, new HashMap<>());
		plateGrid.setConfig(config);

		for (IGridLayer layer : gridLayerSupport.getLayers()) {
			Object layerConfig = properties.getProperty(layer.getName() + GridViewerUtils.CONFIG);
			boolean hasLayerConfig = layerConfig != null;
			if (hasLayerConfig) layer.setConfig(layerConfig);
			layer.toggleEnabled(hasLayerConfig);
		}

		// ProtocolUIService will not respond to 'null' values being set, no 'null' check needed.
		ProtocolUIService.getInstance().setCurrentFeature(PartSettingsUtils.getFeature(properties));
		ProtocolUIService.getInstance().setCurrentNormalization(PartSettingsUtils.getNormalization(properties));

		gridLayerSupport.updateLayers();
	}
	
	private class LoadFeatureDataJob extends Job {

		public LoadFeatureDataJob() {
			super("Load Plate Data");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Loading Plate Data", plates.size());

			try {
				ThreadUtils.runQuery(() -> {
					IntStream.range(0, plates.size()).parallel().forEach(i -> {
						final int plateIndex = i;
						final Plate plate = plates.get(i);

						if (monitor.isCanceled()) return;

						monitor.subTask("Loading plate " + plate.getBarcode());
						CalculationService.getInstance().getAccessor(plate).loadEager(Lists.newArrayList(
								ProtocolUIService.getInstance().getCurrentFeature()));

						if (monitor.isCanceled()) return;

						Display.getDefault().asyncExec(() -> {
							GridViewer viewer = plateGrid.getGridViewer(plateIndex);
							if (viewer != null && !viewer.getControl().isDisposed()) {
								gridLayerSupport.setInput(plate, viewer);
							}
						});
						monitor.worked(1);
					});
				});
			} finally {
				monitor.done();
			}

			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			return Status.OK_STATUS;
		}
	}
}
