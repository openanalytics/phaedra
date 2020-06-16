package eu.openanalytics.phaedra.ui.plate.browser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IProgressMonitor;
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

import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewerUtils;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.MultiGridLayerSupport;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.misc.DNDSupport;
import eu.openanalytics.phaedra.base.ui.util.misc.WorkbenchSiteJobScheduler;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedEditor;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataDirectViewerInput;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataViewerInput;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionProviderIntermediate;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.PlateService;
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
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.ui.protocol.util.ProtocolClasses;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.DynamicColumnSupport;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.EvaluationContext;

public class PlateBrowser extends DecoratedEditor {

	public static final String PLATEGRID_SETTINGS = "SETTINGS_PLATEGRID";
	
	
	private AsyncDataViewerInput<Plate, Plate> viewerInput;
	private volatile Experiment singleExperiment;
	private ProtocolClasses<Plate> protocolClasses;
	private AsyncDataLoader<Plate> dataLoader;
	
	private EvaluationContext<Plate> evaluationContext;
	
	private DataFormatSupport dataFormatSupport;
	
	private BreadcrumbViewer breadcrumb;
	private CTabFolder tabFolder;

	private SelectionProviderIntermediate selectionProvider;

	/* Tab 1: table */
	private CTabItem tableTab;
	private RichTableViewer tableViewer;

	/* Tab 2: plate heatmaps */
	private CTabItem thumbnailsTab;
	private PlateGrid plateGrid;
	private MultiGridLayerSupport gridLayerSupport;
	private boolean plateGridInitialized;

	/* Tab 3: feature heatmaps */
	private CTabItem featureHeatmapsTab;
	private PlateFeatureHeatmapsTab featureHeatmapsTabComposite;
	private boolean featureHeatmapsLoaded;


	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}
	
	private void initViewerInput() {
		this.dataLoader = new AsyncDataLoader<Plate>("data for plate browser",
				new WorkbenchSiteJobScheduler(this) );
		this.viewerInput = new AsyncDataDirectViewerInput<Plate>(Plate.class, this.dataLoader) {
			
			@Override
			protected List<Plate> loadElements() {
				VOEditorInput input = (VOEditorInput)getEditorInput();
				List<IValueObject> valueObjects = input.getValueObjects();
				List<Plate> plates = new ArrayList<>();
				if (!valueObjects.isEmpty()) {
					if (valueObjects.get(0) instanceof Experiment) {
						for (IValueObject vo: valueObjects) plates.addAll(PlateService.getInstance().getPlates((Experiment)vo));
					} else if (valueObjects.get(0) instanceof Plate) {
						for (IValueObject vo: valueObjects) plates.add((Plate)vo);
					}
				}
				if (valueObjects.size() == 1 && valueObjects.get(0) instanceof Experiment) {
					PlateBrowser.this.singleExperiment = (Experiment)valueObjects.get(0);
				}
				return plates;
			}
			
			@Override
			protected void updateElements(final List<Plate> currentElements,
					final Object[] eventElements, final boolean forceUpdateData) {
				if (eventElements == null) {
					super.updateElements(currentElements, eventElements, forceUpdateData);
				}
				if (eventElements.length > 0) {
					VOEditorInput input = (VOEditorInput)getEditorInput();
					if (eventElements[0] instanceof Plate) {
						for (final Object eventElement : eventElements) {
							Plate plate = (Plate)eventElement;
							Experiment experiment = null;
							if (experiment == null) {
								experiment = SelectionUtils.getFirstAsClass(input.getValueObjects(), Experiment.class);
							}
							if (plate.getExperiment().equals(experiment) || currentElements.contains(plate)) {
								super.updateElements(currentElements, eventElements, forceUpdateData);
								return;
							}
						}
					}
					else if (singleExperiment != null && eventElements[0] instanceof Experiment) {
						for (final Object eventElement : eventElements) {
							if (eventElement.equals(singleExperiment)) {
								Display.getDefault().asyncExec(this::updateSingleExperiment);
								return;
							}
						}
					}
				}
			}
			
			private void updateSingleExperiment() {
				Experiment singleExperiment = PlateBrowser.this.singleExperiment;
				if (singleExperiment != null) {
					setPartName(singleExperiment.getName());
					final BreadcrumbViewer breadcrumb = PlateBrowser.this.breadcrumb;
					if (breadcrumb != null && !breadcrumb.getControl().isDisposed()) {
						breadcrumb.refresh();
					}
				}
			}
			
		};
		this.protocolClasses = new ProtocolClasses<>(this.viewerInput,
				(plate) -> plate.getExperiment().getProtocol().getProtocolClass() );
		
		this.evaluationContext = new EvaluationContext<>(this.viewerInput, this.protocolClasses);
	}
	
	private Protocol getProtocol() {
		final Experiment singleExperiment = this.singleExperiment;
		return (singleExperiment != null) ? singleExperiment.getProtocol() : null;
	}
	
	
	@Override
	public void createPartControl(Composite parent) {
		initViewerInput();
		this.dataFormatSupport = new DataFormatSupport(() -> { this.viewerInput.refreshViewer(); reloadHeadmaps(); });
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(container);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(container);
		List<Plate> plates = this.viewerInput.getBaseElements();
		breadcrumb.setInput(plates != null && !plates.isEmpty() ? plates.get(0).getExperiment() : null);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		tabFolder = new CTabFolder(container, SWT.BOTTOM | SWT.V_SCROLL | SWT.H_SCROLL);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
		tabFolder.addListener(SWT.Selection, e -> {
			Widget w = e.item;
			tabChanged(w);
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tabFolder);

		final DynamicColumnSupport<Plate, Plate> customColumnSupport = new DynamicColumnSupport<>(
				this.viewerInput, this.evaluationContext, this.dataFormatSupport );

		tableTab = new CTabItem(tabFolder, SWT.NONE);
		tableTab.setText("Plate List");

		container = new Composite(tabFolder, SWT.NONE);
		container.setLayout(new FillLayout());
		tableTab.setControl(container);

		tableViewer = new RichTableViewer(container, SWT.NONE, getClass().getSimpleName(),
				customColumnSupport, true );
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(PlateTableColumns.configureColumns(this.dataLoader, false, true));
		tableViewer.setDefaultSearchColumn("Barcode");
		
		this.viewerInput.connect(tableViewer);

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
				gridLayerSupport = new MultiGridLayerSupport("hca.singlewell.grid|hca.well.grid", viewer, dataFormatSupport);
				gridLayerSupport.setAttribute("featureProvider", ProtocolUIService.getInstance());
				gridLayerSupport.setAttribute(GridLayerSupport.IS_HIDDEN, getEditorSite().getActionBars().getServiceLocator() == null);
				viewer.setLabelProvider(gridLayerSupport.createLabelProvider());
			} else {
				gridLayerSupport.linkViewer(viewer);
			}
		}
		this.viewerInput.addChangeListener(new IChangeListener() {
			@Override
			public void handleChange(ChangeEvent event) {
				final PlateGrid grid = PlateBrowser.this.plateGrid;
				if (grid != null && !grid.isDisposed()) {
					grid.refreshHeaders();
				}
			}
		});

		/* Feature heatmaps tab */

		featureHeatmapsTab = new CTabItem(tabFolder, SWT.NONE);
		featureHeatmapsTab.setText("Feature Heatmaps");
		featureHeatmapsTabComposite = new PlateFeatureHeatmapsTab(tabFolder, SWT.NONE, plates);
		featureHeatmapsTab.setControl(featureHeatmapsTabComposite);

		/* Other behavior */

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
		
		// Set the current protocolclass, when this editor is opened directly (for example via My Favorites).
		container.getDisplay().asyncExec(() -> {
			final Set<ProtocolClass> pClasses;
			if (getSite().getPage().getActivePart() == this
					&& (pClasses = this.protocolClasses.getValue()).size() == 1) {
				ProtocolUIService.getInstance().setCurrentProtocolClass(pClasses.iterator().next());
			}
		});
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
		if (plateGrid != null) plateGrid.dispose();
		if (gridLayerSupport != null) gridLayerSupport.dispose();
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
			if (!plateGridInitialized) {
				plateGridInitialized = true;
				initPlateGrid();
			}
		} else if (tab == featureHeatmapsTab) {
			selectionProvider.setSelectionProviderDelegate(featureHeatmapsTabComposite.getSelectionProvider());
			if (!featureHeatmapsLoaded) {
				featureHeatmapsLoaded = true;
				featureHeatmapsTabComposite.startLoading();
			}
		}
	}
	
	private void hookDoubleClickAction() {
		tableViewer.addDoubleClickListener(event -> openWellBrowser(event.getSelection()));
	}

	private void openWellBrowser(ISelection selection) {
		Object item = ((StructuredSelection)selection).getFirstElement();
		if (item instanceof Plate) {
			IHandlerService handlerService = getSite().getService(IHandlerService.class);
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


	private boolean hasFeatureLayer() {
		return GridViewerUtils.hasGridLayerEnabled(gridLayerSupport, HeatmapLayer.class, MultiFeatureLayer.class);
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
	
	private void initPlateGrid() {
		this.plateGridInitialized = true;
		this.dataLoader.addDataRequest((plate) -> {
			final PlateDataAccessor plateDataAccessor = CalculationService.getInstance().getAccessor(plate);
			plateDataAccessor.loadEager(Lists.newArrayList(ProtocolUIService.getInstance().getCurrentFeature()));
			return plateDataAccessor;
		});
		this.dataLoader.asyncReload(false);
		this.dataLoader.addListener(new AsyncDataLoader.Listener<Plate>() {
			@Override
			public void onDataLoaded(final List<? extends Plate> elements, final boolean completed) {
				final PlateGrid grid = plateGrid;
				if (grid == null || grid.isDisposed()) {
					return;
				}
				for (final Plate plate : elements) {
					final int idx = viewerInput.getViewerElements().indexOf(plate);
					GridViewer viewer = grid.getGridViewer(idx);
					if (viewer != null && !viewer.getControl().isDisposed()) {
						gridLayerSupport.setInput(plate, viewer);
					}
				}
			}
		});
	}
	
	private void reloadHeadmaps() {
		final List<Plate> plates = this.viewerInput.getBaseElements();
		if (this.plateGrid == null || this.plateGrid.isDisposed()
				|| plates == null) {
			return;
		}
		for (int plateIndex = 0; plateIndex < plates.size(); plateIndex++) {
			GridViewer viewer = plateGrid.getGridViewer(plateIndex);
			if (viewer != null) {
				Object input = viewer.getInput();
				if (input != null) {
					gridLayerSupport.setInput(input, viewer);
				}
			}
		}
	}
	
}
