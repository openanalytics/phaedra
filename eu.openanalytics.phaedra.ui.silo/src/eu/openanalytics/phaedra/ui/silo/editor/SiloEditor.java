package eu.openanalytics.phaedra.ui.silo.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.EditableRule;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.validate.IDataValidator;
import org.eclipse.nebula.widgets.nattable.data.validate.ValidationFailedException;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.hideshow.event.HideColumnPositionsEvent;
import org.eclipse.nebula.widgets.nattable.hideshow.event.ShowColumnPositionsEvent;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;
import org.eclipse.nebula.widgets.nattable.layer.event.RowStructuralRefreshEvent;
import org.eclipse.nebula.widgets.nattable.sort.event.SortColumnEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.Matcher;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.SecurityService.Action;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableBuilder;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.layer.FullFeaturedColumnHeaderLayerStack;
import eu.openanalytics.phaedra.base.ui.nattable.misc.AdvancedNatTableToolTip;
import eu.openanalytics.phaedra.base.ui.nattable.selection.ISelectionTransformer;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionProvider;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.AbstractThumbnailCellRenderer;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.AbstractThumbnailLabelProvider;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.IThumbnailCellRenderer;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.ThumbnailTooltip;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.ThumbnailViewer;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyPasteSelectionDecorator;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.misc.ContextHelper;
import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;
import eu.openanalytics.phaedra.base.ui.util.split.SplitComposite;
import eu.openanalytics.phaedra.base.ui.util.split.SplitCompositeFactory;
import eu.openanalytics.phaedra.base.ui.util.tooltip.IToolTipUpdate;
import eu.openanalytics.phaedra.base.ui.util.tooltip.ToolTipLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedEditor;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionProviderIntermediate;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.util.SiloUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.silo.Activator;
import eu.openanalytics.phaedra.ui.silo.cmd.AbstractSiloCommand;
import eu.openanalytics.phaedra.ui.silo.tree.SiloTreeContentProvider;
import eu.openanalytics.phaedra.ui.silo.tree.SiloTreeLabelProvider;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;

abstract class SiloEditor<ENTITY extends PlatformObject, FEATURE extends IFeature> extends DecoratedEditor {

	private static final int IMAGE_COLUMN_INDEX = 0;
	private static final String IMAGE_COLUMN_NAME = "Image";
	
	private final Matcher<Integer> selectedFilter = new SelectedMatcher();
	private final ViewerFilter selectionFilter = new SelectionFilter();

	private ToolBar toolbar;
	private ToolItem saveItem;
	private ToolItem selectedItem;
	private CTabFolder tabFolder;
	private CTabItem tableTab;
	private CTabItem imageTab;

	private SplitComposite splitComp;
	private Composite treeContainer;
	private Composite imageContainer;
	private Composite tableContainer;

	private TreeViewer treeViewer;

	private boolean isDirty;

	private SelectionProviderIntermediate selectionProvider;
	private ISelectionListener selectionListener;
	private IModelEventListener modelListener;
	private IUIEventListener imageSettingListener;

	private NatTable table;
	private IColumnPropertyAccessor<Integer> tableColumnAccessor;
	private ThumbnailViewer thumbnailViewer;

	private ISiloAccessor<ENTITY> accessor;
	private String currentDatasetName;

	// Image settings.
	private ImageControlPanel imageControlPanel;
	private float scale;
	private boolean[] enabledChannels;

	private Rectangle[] imageBounds;
	private Job imageBoundsJob;

	private NatTableSelectionProvider<Integer> natTableSelectionProvider;
	private FullFeaturedColumnHeaderLayerStack<Integer> columnHeaderLayer;
	private EventList<Integer> eventList;
	private List<Object> currentThumbnailSelection;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		
		Silo silo = SelectionUtils.getFirstAsClass(((VOEditorInput)input).getValueObjects(), Silo.class);
		this.accessor = SiloService.getInstance().getSiloAccessor(silo);
		this.scale = getDefaultScale();
		List<ImageChannel> imageSettings = silo.getProtocolClass().getImageSettings().getImageChannels();
		this.enabledChannels = new boolean[imageSettings.size()];
		for (int i = 0; i < enabledChannels.length; i++) {
			enabledChannels[i] = imageSettings.get(i).isShowInWellView();
		}
		setPartName(silo.getName());

		this.imageBoundsJob = new Job("Retrieving Image Sizes") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					int rowCount = accessor.getRowCount(currentDatasetName);
					monitor.beginTask("Images done: " + 0, rowCount);
					for (int i = 0; i < rowCount; i++) {
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						ENTITY entity = accessor.getRowObject(currentDatasetName, i);
						if (entity == null) continue;
						imageBounds[i] = getImageBounds(entity, scale);
						monitor.setTaskName("Images done: " + i);
						monitor.worked(1);
					}

					Display.getDefault().asyncExec(() -> {
						// Sizes have been changed, update the sizes for the thumbnailViewer and the table (if image column is visible).
						if (thumbnailViewer.getControl().isDisposed() || table.isDisposed()) return;
						thumbnailViewer.resizeImages();
						int imageColumnPos = columnHeaderLayer.getSelectionLayer().getColumnPositionByIndex(IMAGE_COLUMN_INDEX);
						if (imageColumnPos > -1) {
							NatTableUtils.resizeImageColumn(table, natTableSelectionProvider.getRowDataProvider(), eventList, imageColumnPos, imageBounds, 1);
						}
					});
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		this.imageBoundsJob.setUser(false);
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(container);

		Composite toolBarContainer = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(toolBarContainer);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolBarContainer);

		imageControlPanel = new ImageControlPanel(toolBarContainer, SWT.NONE, true, false);
		imageControlPanel.setImage(accessor.getSilo().getProtocolClass());
		imageControlPanel.addImageControlListener(new ImageControlListener() {
			@Override
			public void componentToggled(int component, boolean state) {
				enabledChannels[component] = state;
				updateImages(false);
			}
			@Override
			public void scaleChanged(float ratio) {
				scale = ratio;
				updateImages(true);
			}
		});
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, false).applyTo(imageControlPanel);

		// Create Toolbar
		toolbar = new ToolBar(toolBarContainer, SWT.RIGHT | SWT.WRAP | SWT.FLAT);
		configureToolbar(toolbar);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).grab(true, false).applyTo(toolbar);

		splitComp = SplitCompositeFactory.getInstance().create(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(splitComp);

		// Create TreeViewer
		treeContainer = new Composite(splitComp, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(treeContainer);
		GridLayoutFactory.fillDefaults().applyTo(treeContainer);

		treeViewer = new TreeViewer(treeContainer);
		treeViewer.setContentProvider(new SiloTreeContentProvider());
		treeViewer.setLabelProvider(new SiloTreeLabelProvider());
		treeViewer.setInput(accessor.getSilo());
		treeViewer.addSelectionChangedListener(event -> {
			SiloDataset ds = SelectionUtils.getFirstObject(event.getSelection(), SiloDataset.class);
			if (ds == null) return;
			if (ds.getName().equals(currentDatasetName)) return;
			
			// Clear Current Selections.
			thumbnailViewer.setSelection(new StructuredSelection());
			natTableSelectionProvider.setSelection(new StructuredSelection());
			// Clear List.
			eventList.clear();
			currentDatasetName = ds.getName();
			setInput(true);
		});
		createTreeContextMenu();
		GridDataFactory.fillDefaults().grab(true, true).applyTo(treeViewer.getControl());

		// Create Tab Folder
		tabFolder = new CTabFolder(splitComp, SWT.BOTTOM | SWT.V_SCROLL | SWT.H_SCROLL);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
		tabFolder.addListener(SWT.Selection, e -> tabChanged(e.item));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tabFolder);

		tableContainer = new Composite(tabFolder, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableContainer);
		GridLayoutFactory.fillDefaults().applyTo(tableContainer);

		tableTab = new CTabItem(tabFolder, SWT.NONE);
		tableTab.setText("Table View");
		tableTab.setControl(tableContainer);

		// Nat Table
		createNatTable();

		imageContainer = new Composite(tabFolder, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(imageContainer);
		GridLayoutFactory.fillDefaults().applyTo(imageContainer);

		imageTab = new CTabItem(tabFolder, SWT.NONE);
		imageTab.setText("Image View");
		imageTab.setControl(imageContainer);

		thumbnailViewer = new ThumbnailViewer(imageContainer) {
			@Override
			protected List<Object> getSelectionFromWidget() {
				currentThumbnailSelection = super.getSelectionFromWidget();
				List<Object> entities = new ArrayList<>();
				try {
					int rows = accessor.getRowCount(currentDatasetName);
					for (Object o : currentThumbnailSelection) {
						if (o instanceof Integer) {
							int rowIndex = (int) o;
							if (rowIndex < rows) {
								entities.add(accessor.getRowObject(currentDatasetName, rowIndex));
							}
						}
					}
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
				return entities;
			}
			@Override
			protected void setSelectionToWidget(ISelection selection, boolean reveal) {
				setSelectionToWidget(getSelectedRows(selection), reveal);
			}
		};
		thumbnailViewer.setContentProvider(new ArrayContentProvider());
		thumbnailViewer.setLabelProvider(getThumbnailLabelProvider());
		thumbnailViewer.getThumbnail().addKeyListener(imageControlPanel.getKeyListener());
		thumbnailViewer.getThumbnail().addMouseWheelListener(imageControlPanel.getMouseWheelListener());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(thumbnailViewer.getControl());

		new ThumbnailTooltip(thumbnailViewer, data -> {
			try {
				return accessor.getRowObject(currentDatasetName, (int) data);
			} catch (Exception e) {}
			return null;
		}, createToolTipLabelProvider());

		splitComp.setWeights(new int[] { 20, 80 });
		splitComp.createModeButton().fill(toolbar, 0);

		createSelectionProvider();

		// Table View is default tab
		tabFolder.setSelection(0);
		// Set default scale
		imageControlPanel.setCurrentScale(scale);

		createListeners();

		addDecorator(new CopyPasteSelectionDecorator(CopyPasteSelectionDecorator.COPY | CopyPasteSelectionDecorator.PASTE) {
			@Override
			public void pasteAction(ISelection selection) {
				addEntitySelection(getSelectedEntities(selection));
			}
		});
		initDecorators(parent, thumbnailViewer.getControl());

		ContextHelper.attachContext(thumbnailViewer.getControl(), CopyItems.COPY_PASTE_CONTEXT_ID);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewSiloEditor");
	}

	private void createNatTable() {
		// Create a menu manager.
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));

		// Create table.
		Integer[] rowNrs = new Integer[0];
		try {
			rowNrs = new Integer[accessor.getRowCount(currentDatasetName)];
			for (int i = 0; i < rowNrs.length; i++) {
				rowNrs[i] = i;
			}
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}

		eventList = GlazedLists.eventListOf(rowNrs);

		// Create a NatTable builder.
		tableColumnAccessor = createColumnAccessor();
		NatTableBuilder<Integer> builder = new NatTableBuilder<Integer>(tableColumnAccessor, eventList);
		table = builder
				.hideColumns(new Integer[] { 0 })
				.makeEditable(createDataValidator(), createEditableRule())
				.makeUnsortable(new int[] { 0 })
				.addSortedListEventListener(e -> {
					// Check if the list was sorted.
					if (e.isReordering()) {
						// Get the new sort order.
						EventList<Integer> sourceList = e.getSourceList();
						if (sourceList instanceof SortedList) {
							final Comparator<? super Integer> comparator = ((SortedList<Integer>) sourceList).getComparator();
							ViewerComparator newComparator;
							if (comparator != null) {
								newComparator = new ViewerComparator() {
									@Override
									public int compare(Viewer viewer, Object e1, Object e2) {
										if (e1 instanceof Integer && e2 instanceof Integer) {
											return comparator.compare((Integer) e1, (Integer) e2);
										}
										return super.compare(viewer, e1, e2);
									};
								};
							} else {
								newComparator = null;
							}
							Display.getDefault().asyncExec(() -> thumbnailViewer.setComparator(newComparator));
						}
					}
				})
				.addSelectionProvider(createSelectionTransformer())
				.build(tableContainer, false, menuMgr);
		table.setVisible(!eventList.isEmpty());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		// Needed for Static Filter.
		columnHeaderLayer = builder.getColumnHeaderLayer();
		// Needed for passing on Selections.
		natTableSelectionProvider = builder.getSelectionProvider();
		natTableSelectionProvider.setSelectionConfiguration(ConfigurableStructuredSelection.NO_PARENT);

		table.addLayerListener(new ILayerListener() {
			private boolean hasImageColumn = columnHeaderLayer.getSelectionLayer().getColumnPositionByIndex(IMAGE_COLUMN_INDEX) > -1;
			private Job imageColumnSizeUpdateJob = new Job("Updating Image Column") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					if (hasImageColumn) {
						Display.getDefault().asyncExec(() -> {
							NatTableUtils.resizeImageColumn(table, natTableSelectionProvider.getRowDataProvider(), eventList, -1, imageBounds, 1);
						});
					}
					return Status.OK_STATUS;
				}
			};
			@Override
			public void handleLayerEvent(ILayerEvent event) {
				if (event instanceof RowStructuralRefreshEvent || event instanceof SortColumnEvent) {
					imageColumnSizeUpdateJob.cancel();
					imageColumnSizeUpdateJob.schedule(500);
				}
				if (event instanceof ShowColumnPositionsEvent) {
					// If Image column is being shown, set Image column width to 10.
					if (!hasImageColumn) {
						int imageColumnPos = columnHeaderLayer.getSelectionLayer().getColumnPositionByIndex(IMAGE_COLUMN_INDEX);
						if (imageColumnPos > -1) {
							hasImageColumn = true;
							if (imageBoundsJob.getState() != Job.RUNNING) {
								NatTableUtils.resizeImageColumn(table, natTableSelectionProvider.getRowDataProvider(), eventList, imageColumnPos, imageBounds, 1);
							}
						}
					}
				}
				if (event instanceof HideColumnPositionsEvent) {
					// If Image column is being hidden, set row size back to default.
					if (hasImageColumn && columnHeaderLayer.getSelectionLayer().getColumnPositionByIndex(IMAGE_COLUMN_INDEX) < 0) {
						hasImageColumn = false;
						NatTableUtils.resizeAllRows(table);
					}
				}
			}
		});

		// Use custom Tooltip implementation for Image Preference usage.
		new SiloNatTableToolTip(table, createToolTipLabelProvider());

		// Register menu manager with selection provider.
		getSite().registerContextMenu(menuMgr, natTableSelectionProvider);

		// Add the Selected Filter if enabled.
		if (selectedItem.getSelection()) columnHeaderLayer.addStaticFilter(selectedFilter);
	}

	protected ISelectionTransformer<Integer> createSelectionTransformer() {
		return new ISelectionTransformer<Integer>() {
			@Override
			public List<?> transformOutgoingSelection(List<Integer> list) {
				List<Object> entities = new ArrayList<>();
				try {
					for (int rowIndex : list) {
						entities.add(accessor.getRowObject(currentDatasetName, rowIndex));
					}
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
				return entities;
			}
			@Override
			public List<Integer> transformIngoingSelection(ISelection selection) {
				return getSelectedRows(selection);
			}
		};
	}

	private IColumnPropertyAccessor<Integer> createColumnAccessor() {
		return new IColumnPropertyAccessor<Integer>() {
			@Override
			public Object getDataValue(Integer rowObject, int columnIndex) {
				try {
					if (columnIndex == IMAGE_COLUMN_INDEX) {
						return getImageData(accessor.getRowObject(currentDatasetName, rowObject), scale, enabledChannels);
					}
					
					SiloDataset ds = SiloUtils.getDataset(accessor.getSilo(), currentDatasetName);
					SiloDatasetColumn col = ds.getColumns().get(columnIndex - 1);
					switch (col.getType()) {
					case Float:
						float[] floatValues = accessor.getFloatValues(currentDatasetName, col.getName());
						return (floatValues == null) ? Float.NaN : floatValues[rowObject];
					case String:
						String[] stringValues = accessor.getStringValues(currentDatasetName, col.getName());
						return (stringValues == null) ? null : stringValues[rowObject];
					case Long:
						long[] longValues = accessor.getLongValues(currentDatasetName, col.getName());
						return (longValues == null) ? null : longValues[rowObject];
					default:
						break;
					}
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
				return null;
			}

			@Override
			public void setDataValue(Integer rowIndex, int columnIndex, Object newValue) {
				try {
					if (columnIndex == IMAGE_COLUMN_INDEX) return;
					
					SiloDataset ds = SiloUtils.getDataset(accessor.getSilo(), currentDatasetName);
					SiloDatasetColumn column = ds.getColumns().get(columnIndex - 1);
					
					String stringValue = newValue != null ? newValue.toString() : "";
					Object columnData = null;
					switch (column.getType()) {
					case Float:
						columnData = accessor.getFloatValues(currentDatasetName, column.getName());
						((float[]) columnData)[rowIndex] = Float.parseFloat(stringValue);
						break;
					case String:
						columnData = accessor.getStringValues(currentDatasetName, column.getName());
						((String[]) columnData)[rowIndex] = stringValue;
						break;
					case Long:
						columnData = accessor.getLongValues(currentDatasetName, column.getName());
						((long[]) columnData)[rowIndex] = Long.parseLong(stringValue);
						break;
					default:
						break;
					}
					
					if (columnData != null) accessor.updateValues(currentDatasetName, column.getName(), columnData);
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
			}

			@Override
			public int getColumnCount() {
				SiloDataset ds = getDataset();
				return (ds == null) ? 0 : ds.getColumns().size() + 1;
			}

			@Override
			public String getColumnProperty(int columnIndex) {
				if (columnIndex == IMAGE_COLUMN_INDEX) return IMAGE_COLUMN_NAME;
				else return getColumn(columnIndex).getName();
			}

			@Override
			public int getColumnIndex(String propertyName) {
				if (propertyName.equals(IMAGE_COLUMN_NAME)) return 0;
				List<SiloDatasetColumn> cols = getDataset().getColumns();
				for (int i = 0; i < cols.size(); i++) {
					if (cols.get(i).getName().equals(propertyName)) return i + 1;
				}
				return -1;
			}
		};
	}

	private IDataValidator createDataValidator() {
		return new IDataValidator() {
			@Override
			public boolean validate(int columnIndex, int rowIndex, Object newValue) {
				SiloDatasetColumn col = getColumn(columnIndex);
				switch (col.getType()) {
				case Float:
					if (NumberUtils.isDouble(newValue.toString())) return true;
					else throw new ValidationFailedException("The value should be a decimal number (e.g. 45.6).");
				case Long:
					if (NumberUtils.isDigit(newValue.toString())) return true;
					else throw new ValidationFailedException("The value should be a number (e.g. 45).");
				default:
					break;
				}
				return true;
			}
			
			@Override
			public boolean validate(ILayerCell cell, IConfigRegistry configRegistry, Object newValue) {
				// If the Cell is part of the Filter Row always return true since the Row Index is 0 for this Cell as well.
				if (!cell.getConfigLabels().hasLabel(GridRegion.FILTER_ROW)) {
					return validate(cell.getColumnIndex(), cell.getRowIndex(), newValue);
				}
				return true;
			}
		};
	}

	private EditableRule createEditableRule() {
		return new EditableRule() {
			@Override
			public boolean isEditable(int columnIndex, int rowIndex) {
				return (columnIndex > 0);
			}
		};
	}

	@Override
	public void dispose() {
		imageBoundsJob.cancel();
		getSite().getPage().removeSelectionListener(selectionListener);
		ModelEventService.getInstance().removeEventListener(modelListener);
		ProtocolUIService.getInstance().removeUIEventListener(imageSettingListener);
		if (isDirty) accessor.revert();
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			accessor.save(monitor);
			setDirty(false);
		} catch (SiloException e) {
			if (monitor != null) monitor.isCanceled();
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
	}

	@Override
	public void doSaveAs() {
		// Do nothing.
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
		if (!saveItem.isDisposed()) saveItem.setEnabled(isDirty);
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return SecurityService.getInstance().checkPersonalObject(Action.UPDATE, accessor.getSilo());
	}

	@Override
	public void setFocus() {
		// Do nothing.
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		AbstractSiloCommand.setActiveSilo(accessor.getSilo());
		AbstractSiloCommand.setActiveSiloDataset(currentDatasetName);
		
		int[] cols = columnHeaderLayer.getSelectionLayer().getSelectedColumnPositions();
		String[] colNames = Arrays.stream(cols).mapToObj(i -> tableColumnAccessor.getColumnProperty(i)).toArray(i -> new String[i]);
		AbstractSiloCommand.setActiveColumns(colNames);
		
		super.fillContextMenu(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	protected SiloDataset getDataset() {
		return SiloUtils.getDataset(accessor.getSilo(), currentDatasetName);
	}
	
	protected SiloDatasetColumn getColumn(int columnIndex) {
		return getDataset().getColumns().get(columnIndex - 1);
	}
	
	protected float getDefaultScale() {
		return 1f;
	}

	protected abstract ImageData getImageData(ENTITY entity, float scale, boolean[] channels);
	protected abstract Rectangle getImageBounds(ENTITY entity, float scale);
	protected abstract boolean isImageReady(ENTITY entity, float scale, boolean[] channels);

	protected abstract List<ENTITY> getSelectedEntities(ISelection selection);

	protected abstract void registerDisplayConverters(ISiloAccessor<ENTITY> accessor, String dataGroup, IConfigRegistry configRegistry);

	protected abstract ToolTipLabelProvider createToolTipLabelProvider();

	@SuppressWarnings("unchecked")
	protected void addEntitySelection(List<ENTITY> selections) {
		if (currentDatasetName == null) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "No group selected", "Cannot paste items: no group selected.");
			return;
		}
		try {
			accessor.addRows(currentDatasetName, (ENTITY[]) selections.toArray(new PlatformObject[selections.size()]));
			// Rows are added, consider it dirty.
			setDirty(true);
			setInput(false);
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
	}

	private List<Integer> getSelectedRows(ISelection selection) {
		// Check if the incoming selection is already an Integer List.
		Integer firstObject = SelectionUtils.getFirstObject(selection, Integer.class);
		if (firstObject != null) return SelectionUtils.getObjects(selection, Integer.class);

		List<Integer> list = new ArrayList<>();
		try {
			List<ENTITY> entities = getSelectedEntities(selection);
			for (ENTITY entity : entities) {
				int row = accessor.getIndexOfRow(currentDatasetName, entity);
				if (row > -1) list.add(row);
			}
		} catch (SiloException e) {
			EclipseLog.warn("Failed to retrieve row index", e, Activator.PLUGIN_ID);
		}
		return list;
	}

	private void configureToolbar(ToolBar toolbar) {
		selectedItem = new ToolItem(toolbar, SWT.CHECK);
		selectedItem.setImage(IconManager.getIconImage("funnel.png"));
		selectedItem.setToolTipText("Show selected items / all items");
		selectedItem.addListener(SWT.Selection, e -> {
			if (selectedItem.getSelection()) {
				columnHeaderLayer.addStaticFilter(selectedFilter);
			} else {
				columnHeaderLayer.removeStaticFilter(selectedFilter);
			}
			if (CollectionUtils.find(thumbnailViewer.getFilters(), selectionFilter) == -1) {
				thumbnailViewer.addFilter(selectionFilter);
			} else {
				thumbnailViewer.removeFilter(selectionFilter);
			}
		});

		saveItem = new ToolItem(toolbar, SWT.PUSH);
		saveItem.setImage(IconManager.getIconImage("disk.png"));
		saveItem.setToolTipText("Save the current changes to the silo");
		saveItem.setEnabled(false);
		saveItem.addListener(SWT.Selection, e -> doSave(null));

		ToolItem item = new ToolItem(toolbar, SWT.PUSH);
		item.setImage(IconManager.getIconImage("table.png"));
		item.setToolTipText("Compact table columns");
		item.addListener(SWT.Selection, e -> {
			NatTableUtils.resizeAllColumns(table, DataLayer.DEFAULT_COLUMN_WIDTH);
			int imageColumnPos = columnHeaderLayer.getSelectionLayer().getColumnPositionByIndex(IMAGE_COLUMN_INDEX);
			if (imageColumnPos > -1) {
				NatTableUtils.resizeImageColumn(table, natTableSelectionProvider.getRowDataProvider(), eventList, imageColumnPos, imageBounds, 1);
			} else {
				NatTableUtils.resizeAllRows(table);
			}
		});
	}

	private void createTreeContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> {
			AbstractSiloCommand.setActiveSilo(accessor.getSilo());
			AbstractSiloCommand.setActiveSiloDataset(currentDatasetName);
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(getSite().getId() + ".treeViewer", menuMgr, treeViewer);
	}

	private void setInput(boolean newColumns) {
		// Update rows for NatTable.
		try {
			int currentSize = eventList.size();
			final int rowCount = accessor.getRowCount(currentDatasetName);

			imageBoundsJob.cancel();
			imageBounds = new Rectangle[rowCount];
			for (int i = 0; i < rowCount; i++) {
				imageBounds[i] = new Rectangle(0, 0, 0, 0);
			}
			imageBoundsJob.schedule();

			eventList.getReadWriteLock().writeLock().lock();
			// Use temporary list so we only send out one event instead of n
			List<Integer> rowDiffs = new ArrayList<>(Math.abs(rowCount - currentSize));
			if (currentSize < rowCount) {
				for (int i = currentSize; i < rowCount; i++) {
					rowDiffs.add(i);
				}
				// Add rows to Table.
				eventList.addAll(rowDiffs);
			} else if (rowCount < currentSize) {
				for (int i = currentSize; i > rowCount; i--) {
					rowDiffs.add(i-1);
				}
				// Remove rows from Table.
				eventList.removeAll(rowDiffs);
			}

			// Update columns for NatTable if needed.
			if (newColumns) {
				// Add Column specific display converters.
				if (getDataset() != null) registerDisplayConverters(accessor, currentDatasetName, table.getConfigRegistry());
				table.refresh();
			}
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		} finally {
			eventList.getReadWriteLock().writeLock().unlock();
		}

		table.setVisible(!eventList.isEmpty());
		thumbnailViewer.setInput(eventList);
	}

	private void updateImages(boolean resize) {
		if (resize) {
			// The image size was changed. Update the size array.
			imageBoundsJob.cancel();
			imageBoundsJob.schedule();
		} else {
			// The quality or selected channels was changed. Redraw the components displaying the image.
			thumbnailViewer.getThumbnail().redraw();
			table.doCommand(new VisualRefreshCommand());
		}
	}

	private IBaseLabelProvider getThumbnailLabelProvider() {
		return new AbstractThumbnailLabelProvider() {
			@Override
			public IThumbnailCellRenderer createCellRenderer() {
				return new AbstractThumbnailCellRenderer() {
					@Override
					public ImageData getImageData(Object o) {
						try {
							ENTITY row = accessor.getRowObject(currentDatasetName, (int) o);
							return SiloEditor.this.getImageData(row, scale, enabledChannels);
						} catch (SiloException e) {
							EclipseLog.error(e.getMessage(), e, Activator.getDefault());
						}
						return null;
					}
					@Override
					public Rectangle getImageBounds(Object o) {
						return imageBounds[(int) o];
					}
					@Override
					public boolean isImageReady(Object o) {
						try {
							ENTITY row = accessor.getRowObject(currentDatasetName, (int) o);
							return SiloEditor.this.isImageReady(row, scale, enabledChannels);
						} catch (SiloException e) {
							EclipseLog.error(e.getMessage(), e, Activator.getDefault());
						}
						return false;
					}
				};
			}
		};
	}

	private void tabChanged(Widget tab) {
		// Update the selection provider intermediate.
		if (tab == imageTab) {
			selectionProvider.setSelectionProviderDelegate(thumbnailViewer, false);
		} else if (tab == tableTab) {
			selectionProvider.setSelectionProviderDelegate(natTableSelectionProvider, false);
		}
	}

	private void createListeners() {
		selectionListener = (part, selection) -> {
			if (part == SiloEditor.this) return;

			List<ENTITY> sel = getSelectedEntities(selection);
			if (!sel.isEmpty()) {
				natTableSelectionProvider.setSelection(new StructuredSelection(sel));
				if (CollectionUtils.find(thumbnailViewer.getFilters(), selectionFilter) == -1) {
					thumbnailViewer.setSelection(new StructuredSelection(sel));
				} else {
					thumbnailViewer.refresh();
				}
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);
		
		modelListener = event -> {
			if (event.source instanceof Silo) {
				if (((Silo) event.source).equals(accessor.getSilo())) refreshCurrentSilo();
			}
		};
		ModelEventService.getInstance().addEventListener(modelListener);
		
		imageSettingListener = event -> {
			if (event.type == EventType.ImageSettingsChanged) {
				updateImages(false);
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(imageSettingListener);
	}

	private void refreshCurrentSilo() {
		Display.getDefault().asyncExec(() -> {
			if (treeViewer.getTree().isDisposed()) return;
			setDirty(accessor.isDirty());
			treeViewer.refresh();
			setInput(true);
		});
	}

	private void createSelectionProvider() {
		selectionProvider = new SelectionProviderIntermediate();
		selectionProvider.addSelectionChangedListener(event -> {
			if (event.getSource() == thumbnailViewer) {
				natTableSelectionProvider.setSelection(new StructuredSelection(currentThumbnailSelection));
			} else if (event.getSource() == natTableSelectionProvider) {
				thumbnailViewer.setSelection(new StructuredSelection(natTableSelectionProvider.getCurrentListselection()));
			}
		});
		selectionProvider.setSelectionProviderDelegate(natTableSelectionProvider);

		// Selection provider: switch to treeViewer when it has focus.
		// This is required to make the treeViewer's selection available to its context menu actions.
		treeViewer.getTree().addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				selectionProvider.setSelectionProviderDelegate(treeViewer);
			}
			@Override
			public void focusLost(FocusEvent e) {
				tabChanged(tabFolder.getSelection());
			}
		});

		getSite().setSelectionProvider(selectionProvider);
	}

	private class SelectedMatcher implements Matcher<Integer> {
		@Override
		public boolean matches(Integer row) {
			return natTableSelectionProvider.getCurrentListselection().contains(row);
		}
	}

	private class SelectionFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return currentThumbnailSelection.contains(element);
		}
	}

	public class SiloNatTableToolTip extends AdvancedNatTableToolTip {

		private ToolTipLabelProvider labelProvider;

		public SiloNatTableToolTip(NatTable table, ToolTipLabelProvider labelProvider) {
			super(table);

			this.labelProvider = labelProvider;

			setLabelProvider(new ToolTipLabelProvider() {
				@Override
				public Image getImage(Object element) {
					ILayerCell cell = (ILayerCell) element;
					Object dataValue = cell.getDataValue();

					if (isImageObject(dataValue)) {
						Integer rowIndex = natTableSelectionProvider.getRowDataProvider().getRowObject(cell.getRowIndex());
						try {
							ENTITY row = accessor.getRowObject(currentDatasetName, rowIndex);
							return labelProvider.getImage(row);
						} catch (SiloException e) {
							EclipseLog.error(e.getMessage(), e, Activator.getDefault());
						}
					}

					return null;
				}

				@Override
				public String getText(Object element) {
					ILayerCell cell = (ILayerCell) element;
					Object dataValue = cell.getDataValue();

					String text = null;
					if (isImageObject(dataValue)) {
						Integer rowIndex = natTableSelectionProvider.getRowDataProvider().getRowObject(cell.getRowIndex());
						try {
							ENTITY row = accessor.getRowObject(currentDatasetName, rowIndex);
							text = labelProvider.getText(row);
						} catch (SiloException e) {
							EclipseLog.error(e.getMessage(), e, Activator.getDefault());
						}
					}

					if (text == null) {
						text = getDefaultTextTooltip(cell);
					}

					return text;
				}

				@Override
				public void fillAdvancedControls(Composite parent, Object element, IToolTipUpdate update) {
					ILayerCell cell = (ILayerCell) element;
					Object dataValue = cell.getDataValue();

					if (isImageObject(dataValue)) {
						Integer rowIndex = natTableSelectionProvider.getRowDataProvider().getRowObject(cell.getRowIndex());
						try {
							ENTITY row = accessor.getRowObject(currentDatasetName, rowIndex);
							super.fillAdvancedControls(parent, row, update);
						} catch (SiloException e) {}
					}
				}
			});
		}

		@Override
		public void dispose() {
			super.dispose();
			if (labelProvider != null) labelProvider.dispose();
		}
	}

}