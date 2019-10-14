package eu.openanalytics.phaedra.ui.plate.view;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.hideshow.event.HideColumnPositionsEvent;
import org.eclipse.nebula.widgets.nattable.hideshow.event.ShowColumnPositionsEvent;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.viewport.event.ScrollEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import eu.openanalytics.phaedra.base.datatype.util.DataUnitSupport;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableBuilder;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.convert.FormattedDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.layer.FullFeaturedColumnHeaderLayerStack;
import eu.openanalytics.phaedra.base.ui.nattable.misc.LinkedResizeSupport.SizeManager;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionProvider;
import eu.openanalytics.phaedra.base.ui.nattable.selection.SelectionTransformer;
import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.plate.classification.WellClassificationSupport;
import eu.openanalytics.phaedra.ui.plate.table.WellDataCalculator;
import eu.openanalytics.phaedra.ui.plate.util.WellUtils;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageSettingsDialog;

public class WellDataSetView extends DecoratedView {
	
	
	private DataUnitSupport dataUnitSupport;
	
	private Composite container;
	private NatTable table;
	private FullFeaturedColumnHeaderLayerStack<Well> columnHeaderLayer;
	private NatTableSelectionProvider<Well> selectionProvider;

	private ISelectionListener selectionListener;
	private ISelectionListener highlightListener;
	private IUIEventListener uiEventListener;
	private IModelEventListener modelEventListener;

	private WellClassificationSupport classificationSupport;

	private EventList<Well> eventList;

	private WellDataCalculator columnAccessor;

	private boolean isGroupBy;
	private boolean tableImgColHidden;
	private Point tableImgSize;
	private Job imageColumnSizeUpdateJob;
	private Job highlightJob;
	
	
	@Override
	public void createPartControl(Composite parent) {
		this.dataUnitSupport = new DataUnitSupport(this::reloadData);
		
		container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(2, 2).applyTo(container);

		this.tableImgSize = new Point(20, 20);
		this.columnAccessor = new WellDataCalculator(true, this.dataUnitSupport);
		this.eventList = GlazedLists.eventListOf();

		createTable();
		createJobs();
		createListeners();

		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		addDecorator(new SelectionHandlingDecorator(selectionListener, highlightListener));
		initDecorators(parent);

		SelectionUtils.triggerActiveEditorSelection(selectionListener);
	}

	@Override
	public void setFocus() {
		table.setFocus();
	}

	@Override
	public void dispose() {
		if (this.dataUnitSupport != null) this.dataUnitSupport.dispose();
		if (highlightJob != null) highlightJob.cancel();
		imageColumnSizeUpdateJob.cancel();
		eventList.dispose();
		columnAccessor.dispose();
		ProtocolUIService.getInstance().removeUIEventListener(uiEventListener);
		ModelEventService.getInstance().removeEventListener(modelEventListener);
		getSite().getPage().removeSelectionListener(selectionListener);
		getSite().getPage().removeSelectionListener(highlightListener);
		getSite().getPage().removeSelectionListener(classificationSupport);
		super.dispose();
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		super.fillContextMenu(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator());
		classificationSupport.createContextMenuItem(manager);
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
			}
		});
	}

	@Override
	protected void fillToolbar() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mgr = bars.getToolBarManager();

		mgr.add(new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem groupByItem = new ToolItem(parent, SWT.CHECK);
				groupByItem.setImage(IconManager.getIconImage("table_groupby.png"));
				groupByItem.setToolTipText("Set Table Row Grouping");
				groupByItem.setSelection(isGroupBy);
				groupByItem.addListener(SWT.Selection, e -> {
					isGroupBy = !isGroupBy;

					groupByItem.setToolTipText(isGroupBy ? "Disable Table Row Grouping" : "Set Table Row Grouping");

					// Dispose existing table.
					table.dispose();
					// Create new table and force repaint.
					createTable();
					table.getParent().layout();
				});

				ToolItem item = new ToolItem(parent, SWT.PUSH);
				item.setImage(IconManager.getIconImage("table.png"));
				item.setToolTipText("Compact table");
				item.addListener(SWT.Selection, e -> {
					NatTableUtils.autoResizeAllColumns(table);
					NatTableUtils.resizeAllRows(table);
					imageColumnSizeUpdateJob.cancel();
					imageColumnSizeUpdateJob.schedule();
				});
			}

			@Override
			public boolean isDynamic() {
				return true;
			}
		});

		super.fillToolbar();
	}

	private void createTable() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));

		NatTableBuilder<Well> builder = new NatTableBuilder<Well>(columnAccessor, eventList);
		table = builder
				.addSelectionProvider(new SelectionTransformer<Well>(Well.class))
				.addCustomCellPainters(columnAccessor.getCustomCellPainters())
				.addColumnDialogMatchers(columnAccessor.getColumnDialogMatchers())
				.addConfiguration(columnAccessor.getCustomConfiguration())
				.addLinkedResizeSupport(1f, (w, h) -> changeImageScale(w, h), columnAccessor)
				.resizeColumns(columnAccessor.getColumnWidths())
				.makeUnsortable(new int[] { 0 })
				.makeGroupByable(isGroupBy)
				.build(container, false, menuMgr);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(table);

		columnHeaderLayer = builder.getColumnHeaderLayer();
		selectionProvider = builder.getSelectionProvider();
		selectionProvider.setSelectionConfiguration(ConfigurableStructuredSelection.NO_PARENT);
		table.addLayerListener(event -> {
			if (table.isDisposed()) return;
			if (event instanceof ShowColumnPositionsEvent || event instanceof HideColumnPositionsEvent) {
				int imgColPos = columnHeaderLayer.getSelectionLayer().getColumnPositionByIndex(WellDataCalculator.IMAGE_COLUMN_INDEX);
				boolean imgColHidden = imgColPos < 0;
				if (imgColHidden == tableImgColHidden) return;
				tableImgColHidden = imgColHidden;
				Display.getDefault().asyncExec(() -> {
					if (tableImgColHidden) NatTableUtils.resizeAllRows(table);
					else NatTableUtils.resizeAllRows(table, tableImgSize.y);
				});
			}

			if (event instanceof ScrollEvent) {
				NatTableUtils.preload(table, selectionProvider.getRowDataProvider());
			}
		});
		// We'll be using our own Tooltip implementation here.
		new WellNatTableToolTip(table, columnAccessor, selectionProvider.getRowDataProvider());

		columnAccessor.setTable(table);

		registerDisplayConverters(table.getConfigRegistry());

		getSite().setSelectionProvider(selectionProvider);
		getSite().registerContextMenu(menuMgr, selectionProvider);
	}

	private void createListeners() {
		selectionListener = (IWorkbenchPart part, ISelection selection) -> {
			if (part == WellDataSetView.this) return;

			List<Well> wells = WellUtils.getWells(selection);

			if (!wells.isEmpty() && !CollectionUtils.equalsIgnoreOrder(eventList, wells)) {
				ProtocolClass pClass = eventList.isEmpty() ? null : PlateUtils.getProtocolClass(eventList.get(0));

				columnAccessor.setCurrentWells(wells);
				try {
					eventList.getReadWriteLock().writeLock().lock();
					eventList.clear();
					eventList.addAll(wells);
				} finally {
					eventList.getReadWriteLock().writeLock().unlock();
				}

				// See if the Protocol Class still matches.
				ProtocolClass newPClass = PlateUtils.getProtocolClass(wells.get(0));
				if (newPClass != null && (pClass == null || !pClass.equals(newPClass))) {
					imageColumnSizeUpdateJob.cancel();
					pClass = newPClass;
					columnAccessor.setFeatures(pClass.getFeatures());
					imageColumnSizeUpdateJob.schedule(500);

					registerDisplayConverters(table.getConfigRegistry());
					table.refresh();
				}


				setTitleToolTip(getPartName()
						+ "\nNr. of Rows: " + eventList.size()
						+ "\nNr. of Columns: " + columnAccessor.getColumnCount()
				);
			}
		};

		highlightListener = (part, selection) -> {
			if (part == WellDataSetView.this) return;

			highlightJob = JobUtils.runBackgroundJob(monitor -> {
				List<Well> wells = WellUtils.getWells(selection);
				if (wells.isEmpty() || table.isDisposed()) return;
				Display.getDefault().asyncExec(() -> selectionProvider.setSelection(new StructuredSelection(wells)));
			}, this.toString(), null);
		};

		uiEventListener = event -> {
			if (eventList == null || eventList.isEmpty()) return;
			if (event.type == EventType.FeatureSelectionChanged || event.type == EventType.NormalizationSelectionChanged) {
				ProtocolClass pClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
				ProtocolClass thisPClass = (eventList.get(0)).getPlate().getAdapter(ProtocolClass.class);
				if (thisPClass.equals(pClass)) {
					Display.getDefault().asyncExec(() -> table.refresh());
				}
			} else if (event.type == EventType.ColorMethodChanged) {
				Display.getDefault().asyncExec(() -> table.refresh());
			} else if (event.type == EventType.ImageSettingsChanged) {
				columnAccessor.clearCache();
				reloadData();
			}
		};

		modelEventListener = event -> {
			if (eventList == null || eventList.isEmpty()) return;
			if (event.type == ModelEventType.Calculated) {
				if (event.source instanceof Plate) {
					Plate plate = (Plate)event.source;
					Plate currentPlate = eventList.get(0).getPlate();
					if (currentPlate.equals(plate)) {
						Display.getDefault().asyncExec(() -> table.refresh());
					}
				}
			} else if (event.type == ModelEventType.ValidationChanged) {
				Object[] items = ModelEventService.getEventItems(event);
				List<Plate> plates = SelectionUtils.getAsClass(items, Plate.class);
				Plate currentPlate = eventList.get(0).getPlate();
				if (plates.contains(currentPlate)) {
					Display.getDefault().asyncExec(() -> table.refresh());
				}
			}
		};

		classificationSupport = new WellClassificationSupport();

		ProtocolUIService.getInstance().addUIEventListener(uiEventListener);
		ModelEventService.getInstance().addEventListener(modelEventListener);
		getSite().getPage().addSelectionListener(selectionListener);
		getSite().getPage().addSelectionListener(highlightListener);
		getSite().getPage().addSelectionListener(classificationSupport);
	}
	
	private void reloadData() {
		if (table == null || table.isDisposed()) {
			return;
		}
		table.doCommand(new VisualRefreshCommand());
	}

	private void createJobs() {
		imageColumnSizeUpdateJob = new Job("Updating Image Column") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Display.getDefault().asyncExec(() -> {
					// SizeManager isn't supposed to be accessible, but getImageAspectRatio is so slow (TAU: > 4 sec)
					// it must be delayed until actually needed.
					SizeManager sm = (SizeManager) table.getData("sizeManager");
					if (sm != null) {
						float newAspectRatio = columnAccessor.getImageAspectRatio(eventList);
						if (Math.abs(sm.aspectRatio - newAspectRatio) > 0.001) {
							sm.aspectRatio = newAspectRatio;
							NatTableUtils.resizeColumn(table, 0, tableImgSize.x);
						}
					}
				});

				return Status.OK_STATUS;
			}
		};
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

	private Protocol getProtocol() {
		if (columnAccessor.getCurrentWells() == null) return null;
		return columnAccessor.getCurrentWells().stream().findAny().map(w -> w.getAdapter(Protocol.class)).orElse(null);
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		java.util.Properties tableProperties = new java.util.Properties();
		table.saveState("tableProperties", tableProperties);
		properties.addProperty("tableProperties", tableProperties);
		properties.addProperty("channels", columnAccessor.getChannels());
		properties.addProperty("imageScale", columnAccessor.getScale());
		return properties;
	}
	
	private void setProperties(Properties properties) {
		java.util.Properties tableProperties = properties.getProperty("tableProperties", java.util.Properties.class);
		if (tableProperties != null) table.loadState("tableProperties", tableProperties);
		boolean[] channels = properties.getProperty("channels", boolean[].class);
		if (channels != null) columnAccessor.setChannels(channels);
		float scale = properties.getProperty("imageScale", 1.0f);
		columnAccessor.setScale(scale);
		
		// Image bounds changed, recalculate sizes.
		columnAccessor.clearCache();
		table.doCommand(new VisualRefreshCommand());
	}
}
