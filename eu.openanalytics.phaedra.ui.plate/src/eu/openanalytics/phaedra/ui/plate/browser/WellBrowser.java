package eu.openanalytics.phaedra.ui.plate.browser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.hideshow.event.HideColumnPositionsEvent;
import org.eclipse.nebula.widgets.nattable.hideshow.event.ShowColumnPositionsEvent;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableBuilder;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.convert.FormattedDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.misc.LinkedResizeSupport.SizeManager;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionProvider;
import eu.openanalytics.phaedra.base.ui.nattable.selection.SelectionTransformer;
import eu.openanalytics.phaedra.base.ui.util.misc.DNDSupport;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionProviderIntermediate;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateWellAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.classification.WellClassificationSupport;
import eu.openanalytics.phaedra.ui.plate.grid.PlateContentProvider;
import eu.openanalytics.phaedra.ui.plate.table.WellDataCalculator;
import eu.openanalytics.phaedra.ui.plate.view.WellNatTableToolTip;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageSettingsDialog;

public class WellBrowser extends EditorPart {

	private DataFormatSupport dataFormatSupport;
	
	private CTabFolder tabFolder;
	private CTabItem gridTab;
	private CTabItem tableTab;

	private BreadcrumbViewer breadcrumb;
	private GridViewer gridViewer;
	private NatTable table;
	private GridLayerSupport gridLayerSupport;

	private WellClassificationSupport classificationSupport;

	private PlateWellAccessor wellAccessor;
	private PlateDataAccessor dataAccessor;
	private WellDataCalculator columnAccessor;

	private SelectionProviderIntermediate selectionProvider;
	private ISelectionListener selectionListener;
	private IUIEventListener uiEventListener;
	private IModelEventListener modelEventListener;

	private NatTableSelectionProvider<Well> natTableSelectionProvider;
	private EventList<Well> eventList;
	private Composite tableContainer;
	private MenuManager natTableMenuMgr;

	private boolean tableTabInitialized;
	private boolean tableImgColHidden;
	private boolean isGroupBy;
	private Point tableImgSize;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());

		// Make sure an appropriate feature is selected.
		final Plate plate = getPlate();
		List<Feature> features = PlateUtils.getFeatures(plate);
		Feature f = ProtocolUIService.getInstance().getCurrentFeature();
		if (f == null || !features.contains(f)) {
			f = PlateUtils.getProtocolClass(plate).getDefaultFeature();
			if (f == null) f = CollectionUtils.find(features, ProtocolUtils.KEY_FEATURES);
			// If no Key Features are available, use first feature.
			if (f == null && features.isEmpty()) {
				throw new PartInitException("Cannot open plate: there are currently no well features defined in the Protocol Class.");
			}
			if (f == null) f = features.get(0);
			ProtocolUIService.getInstance().setCurrentProtocolClass(f.getProtocolClass());
			ProtocolUIService.getInstance().setCurrentFeature(f);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		this.dataFormatSupport = new DataFormatSupport(this::reloadData);
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(container);

		final Plate plate = getPlate();
		wellAccessor = new PlateWellAccessor(plate);
		dataAccessor = CalculationService.getInstance().getAccessor(plate);

		classificationSupport = new WellClassificationSupport();
		getSite().getPage().addSelectionListener(classificationSupport);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(container);
		breadcrumb.setInput(plate);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		tabFolder = new CTabFolder(container, SWT.BOTTOM | SWT.V_SCROLL | SWT.H_SCROLL);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
		tabFolder.addListener(SWT.Selection, e -> {
			Widget w = e.item;
			tabChanged(w);
		});
		GridDataFactory.fillDefaults().grab(true,true).applyTo(tabFolder);

		/* Grid Tab */

		gridTab = new CTabItem(tabFolder, SWT.NONE);
		gridTab.setText("Plate View");

		container = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		gridTab.setControl(container);

		gridViewer = new GridViewer(container, plate.getRows(), plate.getColumns());
		GridDataFactory.fillDefaults().grab(true,true).applyTo(gridViewer.getControl());

		gridViewer.setContentProvider(new PlateContentProvider());
		gridLayerSupport = new GridLayerSupport("hca.singlewell.grid|hca.well.grid", gridViewer, dataFormatSupport);
		gridLayerSupport.setAttribute("featureProvider", ProtocolUIService.getInstance());
		gridLayerSupport.setAttribute(GridLayerSupport.IS_HIDDEN, getEditorSite().getActionBars().getServiceLocator() == null);
		gridViewer.setLabelProvider(gridLayerSupport.createLabelProvider());
		gridLayerSupport.setInput(plate);
		gridViewer.getGrid().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.SHIFT) {
					toggleDragSupport(false);
				}
			}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.SHIFT && !gridViewer.getGrid().isDragging()) {
					toggleDragSupport(true);
				}
			}
		});

		createGridViewerContextMenu();

		/* Table tab */

		tableTab = new CTabItem(tabFolder, SWT.NONE);
		tableTab.setText("Table View");

		tableContainer = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableContainer);
		tableTab.setControl(tableContainer);

		natTableMenuMgr = new MenuManager("#Popup");
		natTableMenuMgr.setRemoveAllWhenShown(true);
		natTableMenuMgr.addMenuListener(manager -> fillContextMenu(manager));

		eventList = GlazedLists.eventListOf();

		rebuildTable();

		getSite().registerContextMenu(natTableMenuMgr, natTableSelectionProvider);

		//TODO: DNDSupport.addDragSupport(table, this);

		/* Other */

		selectionProvider = new SelectionProviderIntermediate() {
			@Override
			public ISelection getSelection() {
				ISelection sel = super.getSelection();
				if (sel == null || sel.isEmpty()) sel = new StructuredSelection(getPlate());
				return sel;
			}
		};
		selectionProvider.addSelectionChangedListener(event -> WellBrowser.this.selectionChanged(event));
		selectionProvider.setSelectionProviderDelegate(gridViewer);
		getSite().setSelectionProvider(selectionProvider);

		selectionListener = (part, selection) -> {
			if (part == WellBrowser.this) return;
			List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
			if (wells == null) {
				List<Compound> compounds = SelectionUtils.getObjects(selection, Compound.class);
				if (!compounds.isEmpty()) {
					wells = new ArrayList<Well>();
					for (Compound c: compounds) wells.addAll(c.getWells());
				}
			}
			if (!wells.isEmpty()) {
				selectionProvider.setSelection(new StructuredSelection(wells));
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		uiEventListener = event -> {
			if (event.type == EventType.FeatureSelectionChanged || event.type == EventType.NormalizationSelectionChanged) {
				ProtocolClass pClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
				ProtocolClass thisPClass = getPlate().getAdapter(ProtocolClass.class);
				if (thisPClass.equals(pClass)) {
					gridViewer.setInput(gridViewer.getInput());
				}
			} else if (event.type == EventType.ColorMethodChanged) {
				columnAccessor.resetPainters();
				reloadData();
			} else if (event.type == EventType.ImageSettingsChanged) {
				columnAccessor.clearCache();
				if (tabFolder.getSelection() == tableTab) table.doCommand(new VisualRefreshCommand());
				if (tabFolder.getSelection() == gridTab) gridViewer.refresh();
			} else if (event.type == EventType.FeatureGroupSelectionChanged) {
				rebuildTable();
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(uiEventListener);

		modelEventListener = event -> {
			if (event.type == ModelEventType.Calculated) {
				if (event.source instanceof Plate) {
					Plate calculatedPlate = (Plate)event.source;
					Plate currentPlate = getPlate();
					if (currentPlate.equals(calculatedPlate)) {
						Display.getDefault().asyncExec(refreshUICallback);
					}
				}
			} else if (event.type == ModelEventType.ValidationChanged) {
				Object[] items = ModelEventService.getEventItems(event);
				List<Plate> plates = SelectionUtils.getAsClass(items, Plate.class);
				Plate currentPlate = getPlate();
				if (plates.contains(currentPlate)) {
					Display.getDefault().asyncExec(refreshUICallback);
				}
			} else if (event.type == ModelEventType.ObjectChanged) {
				if (event.source instanceof ProtocolClass) {
					ProtocolClass pClass = (ProtocolClass) event.source;
					ProtocolClass currentPClass = PlateUtils.getProtocolClass(getPlate());
					if (pClass == currentPClass) {
						Display.getDefault().asyncExec(() -> {
							columnAccessor.resetPainters();
							table.doCommand(new VisualRefreshCommand());
						});
					}
				} else if (getPlate().getAdapter(event.source.getClass()) == event.source) {
					Display.getDefault().asyncExec(() -> {
						setPartName(getPlate().toString());
						if (!breadcrumb.getControl().isDisposed()) breadcrumb.refresh();
					});
				}
			}
		};
		ModelEventService.getInstance().addEventListener(modelEventListener);

		tabFolder.setSelection(gridTab);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewWellBrowser");
	}

	@Override
	public void setFocus() {
		gridViewer.getControl().setFocus();
	}
	
	private void reloadData(boolean grid, boolean table) {
		if (this.gridViewer == null || this.gridViewer.getControl().isDisposed()) {
			return;
		}
		if (grid) {
			this.gridLayerSupport.setInput(this.gridViewer.getInput());
		}
		if (table) {
			this.table.doCommand(new VisualRefreshCommand());
		}
	}
	
	private void reloadData() {
		reloadData(true, true);
	}

	@Override
	public void dispose() {
		if (dataFormatSupport != null) dataFormatSupport.dispose();
		gridLayerSupport.dispose();
		if (columnAccessor != null) columnAccessor.dispose();
		eventList.dispose();
		getSite().getPage().removeSelectionListener(selectionListener);
		getSite().getPage().removeSelectionListener(classificationSupport);
		ProtocolUIService.getInstance().removeUIEventListener(uiEventListener);
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

	private Plate getPlate() {
		VOEditorInput input = (VOEditorInput)getEditorInput();
		return SelectionUtils.getFirstAsClass(input.getValueObjects(), Plate.class);
	}

	private void toggleDragSupport(boolean enabled) {
		gridViewer.getGrid().setSelectionEnabled(!enabled);
		DNDSupport.toggleDragSupport(gridViewer, enabled);
	}

	private Runnable refreshUICallback = () -> {
		gridViewer.setInput(gridViewer.getInput());
		table.refresh();
	};

	private void createGridViewerContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));
		Menu menu = menuMgr.createContextMenu(gridViewer.getControl());
		gridViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, gridViewer);
	}

	private void rebuildTable() {
		// Get rid of the previous table and columnAccessor (if any).
		if (columnAccessor != null) columnAccessor.dispose();
		if (table != null && !table.isDisposed()) table.dispose();
		if (!eventList.isEmpty()) {
			// Clear the list in case of rebuild so that size related matters are re-applied properly (SizeManager).
			try {
				eventList.getReadWriteLock().writeLock().lock();
				eventList.clear();
			} finally {
				eventList.getReadWriteLock().writeLock().unlock();
			}
		}

		columnAccessor = new WellDataCalculator(this.dataFormatSupport);
		columnAccessor.setFeatures(ProtocolService.getInstance().getMembers(ProtocolUIService.getInstance().getCurrentFeatureGroup()));
		columnAccessor.setCurrentWells(wellAccessor.getWells());

		Integer[] hiddenColumns = tableImgColHidden ? new Integer[] { 0 } : new Integer[0];

		NatTableBuilder<Well> builder = new NatTableBuilder<>(columnAccessor, eventList);
		table = builder
					.addSelectionProvider(new SelectionTransformer<Well>(Well.class))
					.addCustomCellPainters(columnAccessor.getCustomCellPainters())
					.addColumnDialogMatchers(columnAccessor.getColumnDialogMatchers())
					.addConfiguration(columnAccessor.getCustomConfiguration())
					.addLinkedResizeSupport(1f, (w, h) -> changeImageScale(w, h), columnAccessor)
					.resizeColumns(columnAccessor.getColumnWidths())
					.hideColumns(hiddenColumns)
					.makeUnsortable(new int[] { 0 })
					.makeGroupByable(isGroupBy)
					.build(tableContainer, false, natTableMenuMgr);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		tableContainer.layout();

		natTableSelectionProvider = builder.getSelectionProvider();
		SelectionLayer selectionLayer = builder.getColumnHeaderLayer().getSelectionLayer();

		table.addLayerListener(event -> {
			if (event instanceof ShowColumnPositionsEvent || event instanceof HideColumnPositionsEvent) {
				int imgColPos = selectionLayer.getColumnPositionByIndex(WellDataCalculator.IMAGE_COLUMN_INDEX);
				boolean imgColHidden = imgColPos < 0;
				if (imgColHidden == tableImgColHidden) return;
				tableImgColHidden = imgColHidden;
				Display.getDefault().asyncExec(() -> {
					if (tableImgColHidden) NatTableUtils.resizeAllRows(table);
					else NatTableUtils.resizeAllRows(table, tableImgSize.y);
				});
			}
		});

		// We'll be using our own Tooltip implementation here.
		new WellNatTableToolTip(table, columnAccessor, natTableSelectionProvider.getRowDataProvider());

		columnAccessor.setTable(table);

		// Table tab is no longer initialized.
		tableTabInitialized = false;
		tabChanged(tabFolder.getSelection());
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		// Contributions are added here.
		manager.add(new Separator());
		classificationSupport.createContextMenuItem(manager);
		manager.add(new Separator());
		if (tabFolder.getSelection() == gridTab) gridLayerSupport.contributeContextMenu(manager);
		if (tabFolder.getSelection() == tableTab) createTableContextMenuItem(manager);
	}

	private void createTableContextMenuItem(IMenuManager manager) {
		manager.add(new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem item = new MenuItem(menu, SWT.PUSH);
				item.setText("Image Settings");
				item.setImage(IconManager.getIconImage("image.png"));
				item.addListener(SWT.Selection, e -> {
					if (eventList.isEmpty()) return;
					if (columnAccessor.getChannels() == null) {
						ImageSettings currentSettings = ImageSettingsService.getInstance().getCurrentSettings(eventList.get(0));
						boolean[] channels = new boolean[currentSettings.getImageChannels().size()];
						for (int i = 0; i < channels.length; i++) {
							channels[i] = currentSettings.getImageChannels().get(i).isShowInPlateView();
						}
						columnAccessor.setChannels(channels);
					}

					ImageSettingsDialog dialog = new ImageSettingsDialog(
							Display.getDefault().getActiveShell(), PlateUtils.getProtocolClass(eventList.get(0))
							, columnAccessor.getChannels(), new ImageControlListener() {
								@Override
								public void componentToggled(int component, boolean state) {
									columnAccessor.getChannels()[component] = state;
									columnAccessor.clearCache();
									// Image layers changed, redraw images.
									table.doCommand(new VisualRefreshCommand());
								}
							}
					);
					dialog.open();
				});

				item = new MenuItem(menu, SWT.CHECK);
				item.setText("Set Table Row Grouping");
				item.setImage(IconManager.getIconImage("table_groupby.png"));
				item.setSelection(isGroupBy);
				item.addListener(SWT.Selection, e -> {
					isGroupBy = !isGroupBy;
					rebuildTable();
				});
			}
			@Override
			public boolean isDynamic() {
				return true;
			}
		});
	}

	private void tabChanged(Widget tab) {
		// Update the selection provider intermediate.
		if (tab == gridTab) {
			selectionProvider.setSelectionProviderDelegate(gridViewer);
		} else if (tab == tableTab) {
			selectionProvider.setSelectionProviderDelegate(natTableSelectionProvider);
			if (!tableTabInitialized) initializeTableTab();
		}
	}

	private void initializeTableTab() {
		Job loadDataJob = new Job("Loading Data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading Well Data (Plate " + wellAccessor.getPlate().getBarcode() + ")", IProgressMonitor.UNKNOWN);
				// Load the selected Features eagerly.
				dataAccessor.loadEager(columnAccessor.getFeatures());

				tableTabInitialized = true;
				final float aspectRatio = 1.0f; //Disabled for performance: columnAccessor.getImageAspectRatio(getPlate());

				Display.getDefault().syncExec(() -> {
					if (!table.isDisposed()) {
						// SizeManager isn't supposed to be accessible, but getImageAspectRatio is so slow (TAU: > 4 sec)
						// it must be delayed until actually needed.
						SizeManager sm = (SizeManager) table.getData("sizeManager");
						if (sm != null) sm.aspectRatio = aspectRatio;

						registerDisplayConverters(table.getConfigRegistry());
						try {
							eventList.getReadWriteLock().writeLock().lock();
							eventList.clear();
							eventList.addAll(wellAccessor.getWells());
						} finally {
							eventList.getReadWriteLock().writeLock().unlock();
						}
						// Changed to VisualRefresh in order to support GroupBy.
						table.doCommand(new VisualRefreshCommand());
					}
				});

				return Status.OK_STATUS;
			};
		};
		loadDataJob.setUser(true);
		loadDataJob.schedule();
	}

	private void selectionChanged(SelectionChangedEvent event) {
		// Propagate the selection to the other tabs.
		if (event.getSource() == gridViewer) {
			natTableSelectionProvider.setSelection(event.getSelection());
		} else if (event.getSource() == natTableSelectionProvider) {
			gridViewer.setSelection(event.getSelection());
		}
	}

	private void changeImageScale(int w, int h) {
		columnAccessor.setImageSize(w-2, h-2);
		if (!tableImgColHidden) tableImgSize = columnAccessor.getImageSize();
	}

	private void registerDisplayConverters(IConfigRegistry configRegistry) {
		for (IFeature f : columnAccessor.getFeatures()) {
			if (f != null) {
				String fName = f.getDisplayName();
				String formatString = f.getFormatString();

				FormattedDisplayConverter formattedDisplayConverter = new FormattedDisplayConverter(formatString, false);
				configRegistry.registerConfigAttribute(
						CellConfigAttributes.DISPLAY_CONVERTER
						, formattedDisplayConverter
						, DisplayMode.NORMAL
						, fName
				);

				if (f.isNumeric()) {
					int columnIndex = columnAccessor.getColumnIndex(fName);
					NatTableUtils.applyAdvancedFilter(configRegistry, columnIndex
							, formattedDisplayConverter, formattedDisplayConverter.getFilterComparator());
					NatTableUtils.applySummaryProvider(table, columnAccessor, columnIndex, fName, formattedDisplayConverter);
				}
			}
		}
	}

}
