package eu.openanalytics.phaedra.ui.subwell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.StructuralRefreshCommand;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.filterrow.event.FilterAppliedEvent;
import org.eclipse.nebula.widgets.nattable.hideshow.event.HideColumnPositionsEvent;
import org.eclipse.nebula.widgets.nattable.hideshow.event.ShowColumnPositionsEvent;
import org.eclipse.nebula.widgets.nattable.sort.event.SortColumnEvent;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.Matcher;
import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableBuilder;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.columnChooser.IColumnMatcher;
import eu.openanalytics.phaedra.base.ui.nattable.convert.FormattedDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.layer.FullFeaturedColumnHeaderLayerStack;
import eu.openanalytics.phaedra.base.ui.nattable.misc.AsyncColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.NatTableToolTip.ITooltipColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionProvider;
import eu.openanalytics.phaedra.base.ui.nattable.selection.SelectionTransformer;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.model.subwell.util.SubWellDataChangeListener;
import eu.openanalytics.phaedra.model.subwell.util.SubWellUtils;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageSettingsDialog;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class SubWellDataView extends DecoratedView {

	private static final String IMAGE_COLUMN = "Image";
	private static final int IMAGE_COLUMN_INDEX = 0;

	private BreadcrumbViewer breadcrumb;
	private NatTable table;
	private SubWellColumnAccessor columnAccessor;
	private NatTableSelectionProvider<SubWellItem> selectionProvider;
	private FullFeaturedColumnHeaderLayerStack<SubWellItem> columnHeaderLayer;
	private Matcher<SubWellItem> selectedFilter;
	private boolean isShowSelectedOnly;

	private ISelectionListener selectionListener;
	private IModelEventListener modelEventListener;
	private IUIEventListener imageSettingListener;

	private SubWellClassificationSupport classificationSupport;

	private ProtocolClass pClass;
	private Well currentWell;
	private EventList<SubWellItem> eventList;
	private List<SubWellFeature> features;

	private ICache signalPlotCache;

	private Rectangle[] imageBounds;
	private Job selectionJob;
	private Job imageBoundsJob;
	private Job imageColumnSizeUpdateJob;
	private boolean hasSignalPlots;

	private SubWellDataCalculator calculator;

	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(container);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(container);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		this.calculator = new SubWellDataCalculator();
		this.signalPlotCache = CacheService.getInstance().getDefaultCache();
		this.features = new ArrayList<>();
		this.imageBounds = new Rectangle[0];

		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));

		columnAccessor = new SubWellColumnAccessor();
		eventList = GlazedLists.eventListOf();
		NatTableBuilder<SubWellItem> builder = new NatTableBuilder<SubWellItem>(columnAccessor, eventList);
		table = builder
				.addSelectionProvider(new SubWellSelectionTransformer())
				.addColumnDialogMatchers(getColumnDialogMatchers())
				.resizeColumns(new int[] { 15 })
				.makeUnsortable(new int[] { 0 })
				.build(container, false, menuMgr);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(table);
		columnAccessor.setTable(table);

		columnHeaderLayer = builder.getColumnHeaderLayer();
		selectionProvider = builder.getSelectionProvider();
		table.addLayerListener(event -> {
			if (event instanceof FilterAppliedEvent || event instanceof SortColumnEvent
					|| event instanceof ShowColumnPositionsEvent || event instanceof HideColumnPositionsEvent) {
				// On filter, sort and column show/hide sizes must be updating for images.
				imageColumnSizeUpdateJob.cancel();
				imageColumnSizeUpdateJob.schedule(500);
			}
		});
		// We'll be using our own Tooltip implementation here.
		new SubWellNatTableToolTip(table, columnAccessor, selectionProvider.getRowDataProvider());

		selectedFilter = row -> selectionProvider.getCurrentListselection().contains(row);

		createJobs();
		createListeners();
		createToolbar();

		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		addDecorator(new SelectionHandlingDecorator(selectionListener));
		initDecorators(parent);

		// Obtain an initial selection.
		SelectionUtils.triggerActiveSelection(selectionListener, classificationSupport);

		getSite().registerContextMenu(menuMgr, selectionProvider);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewSubWellData");
	}

	@Override
	public void setFocus() {
		table.setFocus();
	}

	@Override
	public void dispose() {
		columnAccessor.dispose();
		eventList.dispose();
		selectionJob.cancel();
		imageBoundsJob.cancel();
		imageColumnSizeUpdateJob.cancel();
		getSite().getPage().removeSelectionListener(selectionListener);
		getSite().getPage().removeSelectionListener(classificationSupport);
		ModelEventService.getInstance().removeEventListener(modelEventListener);
		ProtocolUIService.getInstance().removeUIEventListener(imageSettingListener);
		super.dispose();
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		classificationSupport.createContextMenuItem(manager);
		manager.add(new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem item = new MenuItem(menu, SWT.PUSH);
				item.setText("Image Settings");
				item.setImage(IconManager.getIconImage("image.png"));
				item.addListener(SWT.Selection, e -> {
					boolean[] channels = calculator.getChannels();
					if (channels == null) {
						ImageSettings currentSettings = ImageSettingsService.getInstance().getCurrentSettings(currentWell);
						channels = new boolean[currentSettings.getImageChannels().size()];
						for (int i = 0; i < channels.length; i++) {
							channels[i] = currentSettings.getImageChannels().get(i).isShowInWellView();
						}
						calculator.setChannels(channels);
					}

					ImageSettingsDialog dialog = new ImageSettingsDialog(
							Display.getDefault().getActiveShell(), PlateUtils.getProtocolClass(currentWell), calculator.getScale(), channels,
							new ImageControlListener() {
								@Override
								public void scaleChanged(float ratio) {
									calculator.setScale(ratio);
									// Image bounds changed, recalculate sizes.
									imageBoundsJob.cancel();
									imageBoundsJob.schedule();
									columnAccessor.reset();
									table.doCommand(new VisualRefreshCommand());
								}
								@Override
								public void componentToggled(int component, boolean state) {
									calculator.getChannels()[component] = state;
									columnAccessor.reset();
									table.doCommand(new VisualRefreshCommand());
								}
							}
					);
					dialog.open();
				});
			}
		});
	}

	private void createListeners() {
		selectionListener = (part, selection) -> {
			if (part == SubWellDataView.this) return;

			Well well = SelectionUtils.getFirstObject(selection, Well.class);
			if (well != null) {
				if (currentWell == null || !currentWell.equals(well)) {
					selectionJob.cancel();

					checkProtocolClass(well);

					currentWell = well;

					selectionJob.schedule(500);
				}
			}

			Object selObj = SelectionUtils.getFirstObject(selection, SubWellSelection.class);
			if (selObj == null) selObj = SelectionUtils.getFirstObject(selection, SubWellItem.class);
			if (selObj != null) {
				if (isShowSelectedOnly) columnHeaderLayer.removeStaticFilter(selectedFilter);
				selectionProvider.setSelection(selection);
				if (isShowSelectedOnly) columnHeaderLayer.addStaticFilter(selectedFilter);
			}
		};

		modelEventListener = new SubWellDataChangeListener() {
			@Override
			public void handleEvent(ModelEvent event) {
				super.handleEvent(event);
				if (event.type == ModelEventType.ObjectChanged) {
					Object o = event.source;
					if (o instanceof ProtocolClass) {
						// Could be a change to one of the SW Features. Refresh table.
						if (o == pClass) {
							// Obtain & sort a list of features.
							features = new ArrayList<>(PlateUtils.getSubWellFeatures(pClass));
							Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);

							Display.getDefault().asyncExec(() -> table.doCommand(new StructuralRefreshCommand()));
						}
					}
				}
			}
			@Override
			protected void handle(List<Well> affectedWells) {
				if (affectedWells.contains(currentWell)) {
					columnAccessor.reset();

					// Obtain & sort a list of features.
					features = new ArrayList<>(PlateUtils.getSubWellFeatures(currentWell.getPlate()));
					Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);

					// Update the table contents.
					Display.getDefault().asyncExec(() -> table.doCommand(new StructuralRefreshCommand()));
				}
			}
		};

		imageSettingListener = event -> {
			if (event.type == EventType.ImageSettingsChanged && currentWell != null) {
				columnAccessor.reset();
				table.doCommand(new VisualRefreshCommand());
			}
		};

		classificationSupport = new SubWellClassificationSupport();

		ModelEventService.getInstance().addEventListener(modelEventListener);
		ProtocolUIService.getInstance().addUIEventListener(imageSettingListener);
		getSite().getPage().addSelectionListener(selectionListener);
		getSite().getPage().addSelectionListener(classificationSupport);
		getSite().setSelectionProvider(selectionProvider);
	}

	private void checkProtocolClass(Well well) {
		ProtocolClass newPClass = PlateUtils.getProtocolClass(well);
		if (pClass == null || !pClass.equals(newPClass)) {
			pClass = newPClass;

			// Obtain & sort a list of features.
			features = new ArrayList<>(pClass.getSubWellFeatures());
			Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);

			calculator.setChannels(null);
			registerDisplayConverters(table.getConfigRegistry());
			for (String signalPlot : SubWellDataCalculator.SIGNAL_PLOTS) {
				SubWellFeature f = ProtocolUtils.getSubWellFeatureByName(signalPlot, newPClass);
				if (f != null) {
					hasSignalPlots = true;
					break;
				}
			}
			table.doCommand(new StructuralRefreshCommand());
		}
	}

	private void createToolbar() {
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem item = new ToolItem(parent, SWT.CHECK);
				item.setImage(IconManager.getIconImage("wand.png"));
				item.setToolTipText("Show selected items / all items");
				item.addListener(SWT.Selection, e -> {
					isShowSelectedOnly = ((ToolItem) e.widget).getSelection();
					if (isShowSelectedOnly) {
						columnHeaderLayer.addStaticFilter(selectedFilter);
					} else {
						columnHeaderLayer.removeStaticFilter(selectedFilter);
					}
				});

				item = new ToolItem(parent, SWT.PUSH);
				item.setImage(IconManager.getIconImage("table.png"));
				item.setToolTipText("Compact table");
				item.addListener(SWT.Selection, e -> {
					NatTableUtils.autoResizeAllColumns(table);
					NatTableUtils.resizeAllRows(table);
					imageColumnSizeUpdateJob.cancel();
					imageColumnSizeUpdateJob.schedule();
				});
			}
		};
		mgr.add(contributionItem);
	}

	private Map<String, IColumnMatcher> getColumnDialogMatchers() {
		Map<String, IColumnMatcher> columnMatchers = new LinkedHashMap<>();

		columnMatchers.put("Select All", col -> true);
		columnMatchers.put("Select Key Features", col -> {
			int colIndex = col.getIndex() - 1;
			if (colIndex >= 0) return features.get(colIndex).isKey();
			return false;
		});
		columnMatchers.put("Select Numeric Features", col -> {
			int colIndex = col.getIndex() - 1;
			if (colIndex >= 0) return features.get(colIndex).isNumeric();
			return false;
		});

		return columnMatchers;
	}

	private ImageData getSignalPlot(SubWellFeature feature, int cellNr) {
		CacheKey key = CacheKey.create("SignalPlot", feature, currentWell, cellNr);
		Object o = signalPlotCache.get(key);
		if (o == null) {
			o = signalPlotCache.put(key, calculator.createSignalPlot(currentWell, feature, cellNr));
		}
		return (ImageData) o;
	}

	private void createJobs() {
		selectionJob = new Job(getPartName() + ": Setting Input") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				imageBoundsJob.cancel();

				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				int size = SubWellService.getInstance().getNumberOfCells(currentWell);
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				try {
					eventList.getReadWriteLock().writeLock().lock();
					eventList.clear();
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					for (int i = 0; i < size; i++) {
						eventList.add(new SubWellItem(currentWell, i));
					}
				} finally {
					eventList.getReadWriteLock().writeLock().unlock();
				}

				if (monitor.isCanceled()) return Status.CANCEL_STATUS;

				calculator.setCurrentWell(currentWell);
				Display.getDefault().syncExec(() -> {
					if (breadcrumb.getControl().isDisposed()) return;
					// Update the breadcrumb
					breadcrumb.setInput(currentWell);
					breadcrumb.getControl().getParent().layout();

					setPartName("Subwell Data - " + NumberUtils.getWellCoordinate(currentWell.getRow(), currentWell.getColumn()));
					setTitleToolTip(getPartName()
							+ "\nNr. of Rows: " + eventList.size()
							+ "\nNr. of Columns: " + columnAccessor.getColumnCount()
					);
				});

				imageBoundsJob.schedule(500);

				monitor.done();
				return Status.OK_STATUS;
			}
		};

		imageBoundsJob = new Job(getPartName() + ": Retrieving Image Sizes") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				int rowCount = eventList.size();
				imageBounds = new Rectangle[rowCount];
				monitor.beginTask("Images done: " + 0, rowCount);
				for (int i = 0; i < rowCount; i++) {
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					imageBounds[i] = ImageRenderService.getInstance().getSubWellImageBounds(currentWell, i, calculator.getScale());
					// When Signal Plots are present, make sure the minimum height is 48.
					if (hasSignalPlots) imageBounds[i].height = Math.max(imageBounds[i].height, 48);
					imageBounds[i].height++;
					imageBounds[i].width++;
					monitor.setTaskName("Images done: " + i + "/" + rowCount);
					monitor.worked(1);
				}
				// Now use the retrieved image sizes to update image column.
				imageColumnSizeUpdateJob.cancel();
				imageColumnSizeUpdateJob.schedule();

				monitor.done();
				return Status.OK_STATUS;
			}
		};
		imageColumnSizeUpdateJob = new Job(getPartName() + ": Updating Image Column") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final int imageColumnPos = columnHeaderLayer.getSelectionLayer().getColumnPositionByIndex(IMAGE_COLUMN_INDEX);
				Display.getDefault().asyncExec(() -> {
					if (imageColumnPos > -1) {
						if (imageBounds.length == 0 || imageBounds[imageBounds.length-1] == null) return;
						NatTableUtils.resizeImageColumn(table, selectionProvider.getRowDataProvider(), eventList, imageColumnPos, imageBounds, 0);
					} else {
						if (hasSignalPlots) NatTableUtils.resizeAllRows(table, 48);
						else NatTableUtils.resizeAllRows(table);
					}
				});

				monitor.done();
				return Status.OK_STATUS;
			}
		};
	}

	private void registerDisplayConverters(IConfigRegistry configRegistry) {
		for (IFeature f : features) {
			if (f != null) {
				String formatString = f.getFormatString();
				FormattedDisplayConverter formattedDisplayConverter = new FormattedDisplayConverter(formatString, false);
				configRegistry.registerConfigAttribute(
						CellConfigAttributes.DISPLAY_CONVERTER
						, formattedDisplayConverter
						, DisplayMode.NORMAL
						, f.getDisplayName()
				);
				if (f.isNumeric()) {
					String fName = f.getDisplayName();
					int columnIndex = columnAccessor.getColumnIndex(fName);
					NatTableUtils.applyAdvancedFilter(configRegistry, columnIndex
							, formattedDisplayConverter, formattedDisplayConverter.getFilterComparator());
				}
			}
		}
	}

	private Protocol getProtocol() {
		if (currentWell == null) return null;
		return (Protocol) currentWell.getAdapter(Protocol.class);
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		java.util.Properties tableProperties = new java.util.Properties();
		table.saveState("tableProperties", tableProperties);
		properties.addProperty("tableProperties", tableProperties);
		properties.addProperty("channels", calculator.getChannels());
		properties.addProperty("imageScale", calculator.getScale());
		return properties;
	}
	
	private void setProperties(Properties properties) {
		java.util.Properties tableProperties = properties.getProperty("tableProperties", java.util.Properties.class);
		if (tableProperties != null) table.loadState("tableProperties", tableProperties);
		boolean[] channels = properties.getProperty("channels", boolean[].class);
		if (channels != null) calculator.setChannels(channels);
		float scale = properties.getProperty("imageScale", 1.0f);
		calculator.setScale(scale);
		
		// Image bounds changed, recalculate sizes.
		imageBoundsJob.cancel();
		imageBoundsJob.schedule();
		columnAccessor.reset();
		table.doCommand(new VisualRefreshCommand());
	}
	
	private class SubWellSelectionTransformer extends SelectionTransformer<SubWellItem> {
		@Override
		public List<SubWellItem> transformIngoingSelection(ISelection selection) {
			return SubWellUtils.getSubWellItems(selection);
		}
	}

	private class SubWellColumnAccessor extends AsyncColumnAccessor<SubWellItem> implements ITooltipColumnAccessor<SubWellItem> {
		@Override
		public Object getDataValue(SubWellItem rowObject, int columnIndex) {
			if (columnIndex != IMAGE_COLUMN_INDEX && columnIndex < getColumnCount()) {
				SubWellFeature f = features.get(columnIndex - 1);
				if (CollectionUtils.find(SubWellDataCalculator.SIGNAL_PLOTS, f.getName()) >= 0) {
					return getSignalPlot(f, rowObject.getIndex());
				}
				if (f.isNumeric()) {
					float[] data = SubWellService.getInstance().getNumericData(rowObject.getWell(), f);
					if (data != null && rowObject.getIndex() < data.length) {
						return data[rowObject.getIndex()];
					}
				} else {
					String[] data = SubWellService.getInstance().getStringData(rowObject.getWell(), f);
					if (data != null && rowObject.getIndex() < data.length) {
						return data[rowObject.getIndex()];
					}
				}
			}
			return super.getDataValue(rowObject, columnIndex);
		}
		@Override
		protected Object loadDataValue(SubWellItem rowObject, int columnIndex) {
			if (columnIndex == IMAGE_COLUMN_INDEX) {
				try {
					return ImageRenderService.getInstance().getSubWellImageData(currentWell, rowObject.getIndex(), calculator.getScale(), calculator.getChannels());
				} catch (IOException e) {
					Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage()));
				}
			}
			return super.loadDataValue(rowObject, columnIndex);
		}
		@Override
		public int getColumnCount() {
			return features.size() + 1;
		}
		@Override
		public String getColumnProperty(int columnIndex) {
			if (columnIndex == IMAGE_COLUMN_INDEX) return IMAGE_COLUMN;
			return features.get(columnIndex - 1).getDisplayName();
		}
		@Override
		public int getColumnIndex(String propertyName) {
			if (IMAGE_COLUMN.equals(propertyName)) return IMAGE_COLUMN_INDEX;
			for (int i = 0; i < features.size(); i++) {
				if (features.get(i).getName().equalsIgnoreCase(propertyName)
						|| features.get(i).getDisplayName().equalsIgnoreCase(propertyName)) {

					return i + 1;
				}
			}
			return -1;
		}
		@Override
		public String getTooltipText(SubWellItem rowObject, int colIndex) {
			if (rowObject == null) {
				if (colIndex >= 1) return features.get(colIndex - 1).getName();
			}
			return null;
		}
	}

}
