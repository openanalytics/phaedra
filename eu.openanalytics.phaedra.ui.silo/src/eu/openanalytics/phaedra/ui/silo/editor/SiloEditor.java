package eu.openanalytics.phaedra.ui.silo.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
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
import eu.openanalytics.phaedra.base.event.ModelEvent;
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
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.SiloDataService;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.util.ColumnDescriptor;
import eu.openanalytics.phaedra.silo.util.SiloStructure;
import eu.openanalytics.phaedra.silo.util.SiloStructureUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;
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

	private Listener[] keyListeners;

	private SelectionProviderIntermediate selectionProvider;
	private ISelectionListener selectionListener;
	private IModelEventListener modelListener;
	private IUIEventListener imageSettingListener;

	private NatTable table;
	private ThumbnailViewer thumbnailViewer;

	private Silo silo;
	private ISiloAccessor<ENTITY> accessor;
	private String dataGroup;

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
		this.silo = SelectionUtils.getFirstAsClass(((VOEditorInput)input).getValueObjects(), Silo.class);
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
					int rowCount = accessor.getRowCount(dataGroup);
					monitor.beginTask("Images done: " + 0, rowCount);
					for (int i = 0; i < rowCount; i++) {
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						ENTITY entity = accessor.getRow(dataGroup, i);
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
		imageControlPanel.setImage(silo.getProtocolClass());
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
		treeViewer.setInput(silo);
		treeViewer.addSelectionChangedListener(event -> {
			final SiloStructure struct = SelectionUtils.getFirstObject(event.getSelection(), SiloStructure.class);
			if (struct == null || struct.isDataset()) return;
			if (struct.getFullName().equals(dataGroup)) return;
			Job job = new Job("Load Silo Data") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Loading Silo Data", IProgressMonitor.UNKNOWN);
					try {
						loadGroup(struct.getFullName());
					} catch (IOException e) {
						EclipseLog.warn(e.getMessage(), e, Activator.getDefault());
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
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
		createNatTable(false);

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
					int rows = accessor.getRowCount(dataGroup);
					for (Object o : currentThumbnailSelection) {
						if (o instanceof Integer) {
							int rowIndex = (int) o;
							if (rowIndex < rows) {
								entities.add(accessor.getRow(dataGroup, rowIndex));
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
		thumbnailViewer.getThumbnail().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.SHIFT) {
					toggleDragSupport(false);
				}
			}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.SHIFT && !thumbnailViewer.getThumbnail().isDragging()) {
					toggleDragSupport(true);
				}
			}
		});
		thumbnailViewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { LocalSelectionTransfer.getTransfer() }, createDropListener());
		thumbnailViewer.getThumbnail().addKeyListener(imageControlPanel.getKeyListener());
		thumbnailViewer.getThumbnail().addMouseWheelListener(imageControlPanel.getMouseWheelListener());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(thumbnailViewer.getControl());

		new ThumbnailTooltip(thumbnailViewer, data -> {
			try {
				return accessor.getRow(dataGroup, (int) data);
			} catch (Exception e) {}
			return null;
		}, createToolTipLabelProvider());

		splitComp.setWeights(new int[] { 20, 80 });
		splitComp.createModeButton().fill(toolbar, 0);

		initializeKeyListeners();
		createSelectionProvider();

		// Table View is default tab
		tabFolder.setSelection(0);
		// Set default scale
		imageControlPanel.setCurrentScale(scale);

		createListeners();

		addDecorator(new CopyPasteSelectionDecorator(CopyPasteSelectionDecorator.COPY | CopyPasteSelectionDecorator.CUT | CopyPasteSelectionDecorator.PASTE) {
			@Override
			public void pasteAction(ISelection selection) {
				addEntitySelection(getSelectedEntities(selection));
			}
			@Override
			public void cutAction(ISelection selection) {
				try {
					List<SiloStructure> siloStructures = SelectionUtils.getObjects(selection, SiloStructure.class);
					String cutGroup;
					// Check if the cut was done in another group of this Silo.
					if (siloStructures.isEmpty()) {
						cutGroup = dataGroup;
					} else {
						cutGroup = siloStructures.get(0).getFullName();
					}
					List<ENTITY> rowsToDelete = getSelectedEntities(selection);
					int[] rows = new int[rowsToDelete.size()];
					for (int i=0; i<rows.length; i++) {
						rows[i] = accessor.getRow(cutGroup, rowsToDelete.get(i));
					}
					accessor.removeRows(cutGroup, rows);
					if (dataGroup.equals(cutGroup))	{
						selectionProvider.setSelection(new StructuredSelection(rowsToDelete));
					}
				} catch (SiloException e) {
					Shell shell = Display.getDefault().getActiveShell();
					String msg = "Failed to delete rows";
					ErrorDialog.openError(shell, "Cannot Delete Rows", msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
				}
			}
		});
		initDecorators(parent, thumbnailViewer.getControl());

		ContextHelper.attachContext(thumbnailViewer.getControl(), CopyItems.COPY_PASTE_CONTEXT_ID);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewSiloEditor");
	}

	private void createNatTable(boolean isGroupBy) {
		// Create a menu manager.
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));

		// Create table.
		Integer[] rowNrs = new Integer[0];
		try {
			rowNrs = new Integer[accessor.getRowCount(dataGroup)];
			for (int i = 0; i < rowNrs.length; i++) {
				rowNrs[i] = i;
			}
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}

		eventList = GlazedLists.eventListOf(rowNrs);

		// Create a NatTable builder.
		NatTableBuilder<Integer> builder = new NatTableBuilder<Integer>(createColumnAccessor(), eventList);
		table = builder
				.hideColumns(new Integer[] { 0 })
				.makeEditable(createDataValidator(), createEditableRule())
				.makeGroupByable(isGroupBy)
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

		// Add Drag & Drop support.
		// TODO: Find way to disable selection so it does not ruin selection.
		//table.addDragSupport(DND.DROP_MOVE, new Transfer[] { LocalSelectionTransfer.getTransfer() },
		//		createDragListener());
		table.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { LocalSelectionTransfer.getTransfer() },
				createDropListener());

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
						entities.add(accessor.getRow(dataGroup, rowIndex));
					}
					SiloStructure siloStructure = SiloStructureUtils.findDataGroup(SiloDataService.getInstance().getSiloStructure(silo), dataGroup);
					entities.add(siloStructure);
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
			// We forcefully add an Image column. Hence the +1/-1.
			private static final String IMAGE_COLUMN = "Image";

			@Override
			public Object getDataValue(Integer rowObject, int columnIndex) {
				try {
					if (columnIndex == IMAGE_COLUMN_INDEX) return getImageData(accessor.getRow(dataGroup, rowObject), scale, enabledChannels);

					SiloDataType type = accessor.getDataType(dataGroup, columnIndex-1);
					switch (type) {
					case Float:
						return accessor.getFloatValues(dataGroup, columnIndex-1)[rowObject];
					case String:
						return accessor.getStringValues(dataGroup, columnIndex-1)[rowObject];
					case Integer:
						return accessor.getIntValues(dataGroup, columnIndex-1)[rowObject];
					case Long:
						return accessor.getLongValues(dataGroup, columnIndex-1)[rowObject];
					case Double:
						return accessor.getDoubleValues(dataGroup, columnIndex-1)[rowObject];
					default:
						break;
					}
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
				return null;
			}

			@Override
			public void setDataValue(Integer rowObject, int columnIndex, Object newValue) {
				try {
					if (columnIndex == IMAGE_COLUMN_INDEX) return;

					String stringValue = newValue != null ? newValue.toString() : "";
					Object columnData = null;
					SiloDataType type = accessor.getDataType(dataGroup, columnIndex-1);

					switch (type) {
					case Float:
						columnData = accessor.getFloatValues(dataGroup, columnIndex-1);
						((float[])columnData)[rowObject] = Float.parseFloat(stringValue);
						break;
					case String:
						columnData = accessor.getStringValues(dataGroup, columnIndex-1);
						((String[])columnData)[rowObject] = stringValue;
						break;
					case Integer:
						columnData = accessor.getIntValues(dataGroup, columnIndex-1);
						((int[])columnData)[rowObject] = Integer.parseInt(stringValue);
						break;
					case Long:
						columnData = accessor.getLongValues(dataGroup, columnIndex-1);
						((long[])columnData)[rowObject] = Long.parseLong(stringValue);
						break;
					case Double:
						columnData = accessor.getDoubleValues(dataGroup, columnIndex-1);
						((double[])columnData)[rowObject] = Double.parseDouble(stringValue);
						break;
					default:
						break;
					}

					// Values have been changed. Trigger event and make Editor dirty.
					setDirty(true);
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
			}

			@Override
			public int getColumnCount() {
				try {
					return accessor.getColumns(dataGroup).length + 1;
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
				return 0;
			}

			@Override
			public String getColumnProperty(int columnIndex) {
				try {
					if (columnIndex == IMAGE_COLUMN_INDEX) return IMAGE_COLUMN;
					else return accessor.getColumns(dataGroup)[columnIndex-1];
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
				return null;
			}

			@Override
			public int getColumnIndex(String propertyName) {
				try {
					if (propertyName.equals(IMAGE_COLUMN)) return 0;
					String[] columns = accessor.getColumns(dataGroup);
					for (int i = 0; i < columns.length; i++) {
						if (columns[i].equals(propertyName)) {
							return i+1;
						}
					}
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
				return 0;
			}
		};
	}

	private IDataValidator createDataValidator() {
		return new IDataValidator() {
			@Override
			public boolean validate(int columnIndex, int rowIndex, Object newValue) {
				try {
					SiloDataType dataType = accessor.getDataType(dataGroup, columnIndex-1);
					switch (dataType) {
					case Double:
					case Float:
						if (NumberUtils.isDouble(newValue.toString())) {
							return true;
						} else {
							throw new ValidationFailedException("The value should be a decimal number (e.g. 45.6).");
						}
					case Integer:
					case Long:
						if (NumberUtils.isDigit(newValue.toString())) {
							return true;
						} else {
							throw new ValidationFailedException("The value should be a number (e.g. 45).");
						}
					default:
						break;
					}
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
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
				if (columnIndex == 0) return false;
				try {
					String[] columns = accessor.getColumns(dataGroup);
					return accessor.isEditable(columns[columnIndex-1]);
				} catch (SiloException e) {
					EclipseLog.error(e.getMessage(), e, Activator.getDefault());
				}
				return false;
			}
		};
	}

	@Override
	public void dispose() {
		imageBoundsJob.cancel();
		getSite().getPage().removeSelectionListener(selectionListener);
		ModelEventService.getInstance().removeEventListener(modelListener);
		ProtocolUIService.getInstance().removeUIEventListener(imageSettingListener);
		Display.getDefault().removeFilter(SWT.KeyDown, keyListeners[0]);
		Display.getDefault().removeFilter(SWT.KeyUp, keyListeners[1]);
		if (isDirty) {
			try {
				// Revert the changes.
				accessor.revert();
			} catch (SiloException e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
			}
		}
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
		return SecurityService.getInstance().checkPersonalObject(Action.UPDATE, silo);
	}

	@Override
	public void setFocus() {
		// Do nothing.
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		AbstractSiloCommand.setActiveSilo(silo);
		AbstractSiloCommand.setActiveSiloGroup(dataGroup);
		super.fillContextMenu(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	protected float getDefaultScale() {
		return 1f;
	}

	protected abstract ImageData getImageData(ENTITY entity, float scale, boolean[] channels);
	protected abstract Rectangle getImageBounds(ENTITY entity, float scale);
	protected abstract boolean isImageReady(ENTITY entity, float scale, boolean[] channels);

	/**
	 * Converts the given Selection to a List<ENTITY>.
	 *
	 * @param selection
	 * @return
	 */
	protected abstract List<ENTITY> getSelectedEntities(ISelection selection);

	protected abstract boolean selectFeatures(ProtocolClass pClass, List<FEATURE> selectedFeatures, List<String> selectedNormalizations);

	protected abstract void registerDisplayConverters(ISiloAccessor<ENTITY> accessor, String dataGroup, IConfigRegistry configRegistry);

	protected abstract ToolTipLabelProvider createToolTipLabelProvider();

	protected void configureFeatures(final ISiloAccessor<ENTITY> accessor, final String dataGroup) {
		final ProtocolClass pClass = accessor.getSilo().getProtocolClass();
		final List<FEATURE> selectedFeatures = new ArrayList<>();
		final List<String> selectedNormalizations = new ArrayList<>();

		// Open the Feature Selection dialog.
		boolean proceed = selectFeatures(pClass, selectedFeatures, selectedNormalizations);
		if (!proceed) return;

		if (dataGroup == null) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "No group selected", "Cannot configure features: no group selected.");
			return;
		}

		Job addColumnsJob = new Job("Adding Columns") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Adding Columns", selectedFeatures.size());
				try {
					// Check for Features that need to be added to the Silo.
					for (int i=0; i<selectedFeatures.size(); i++) {
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						FEATURE f = selectedFeatures.get(i);
						monitor.subTask("Adding column " + (i+1) + "/" + selectedFeatures.size() + ": " + f.getName() + "");
						String norm = selectedNormalizations.get(i);
						String colName = ColumnDescriptor.createColumnName(f, norm);
						if (!CollectionUtils.contains(accessor.getColumns(dataGroup), colName)) {
							accessor.addColumn(dataGroup, colName);
						}
						monitor.worked(1);
					}
				} catch (SiloException e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to add columns", e);
				}

				monitor.done();
				return Status.OK_STATUS;
			}
		};
		addColumnsJob.setUser(true);
		addColumnsJob.schedule();
	}

	@SuppressWarnings("unchecked")
	protected void addEntitySelection(List<ENTITY> selections) {
		if (dataGroup == null) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "No group selected", "Cannot paste items: no group selected.");
			return;
		}
		try {
			accessor.addRows(dataGroup, (ENTITY[]) selections.toArray(new PlatformObject[selections.size()]));
			// Rows are added, consider it dirty.
			setDirty(true);
			setInput(false);
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
	}


	/*
	 * Private Methods
	 */

	private List<Integer> getSelectedRows(ISelection selection) {
		// Check if the incoming selection is already an Integer List.
		Integer firstObject = SelectionUtils.getFirstObject(selection, Integer.class);
		if (firstObject != null) return SelectionUtils.getObjects(selection, Integer.class);

		List<Integer> list = new ArrayList<>();
		try {
			List<ENTITY> entities = getSelectedEntities(selection);
			for (ENTITY entity : entities) {
				int row = accessor.getRow(dataGroup, entity);
				if (row > -1) list.add(row);
			}
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
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
//			thumbnailViewer.setInput(thumbnailViewer.getInput());
//			thumbnailViewer.refresh();
		});

		saveItem = new ToolItem(toolbar, SWT.PUSH);
		saveItem.setImage(IconManager.getIconImage("disk.png"));
		saveItem.setToolTipText("Save the current table to the HDF5 File");
		saveItem.setEnabled(false);
		saveItem.addListener(SWT.Selection, e -> doSave(null));

		ToolItem item = new ToolItem(toolbar, SWT.PUSH);
		item.setImage(IconManager.getIconImage((silo.getType() == GroupType.WELL.getType()) ? "tag_blue.png" : "tag_red.png"));
		item.setToolTipText("Select which Features should be included in the Silo");
		item.addListener(SWT.Selection, e -> configureFeatures(accessor, dataGroup));

		item = new ToolItem(toolbar, SWT.PUSH);
		item.setImage(IconManager.getIconImage("table.png"));
		item.setToolTipText("Compact table");
		item.addListener(SWT.Selection, e -> {
			NatTableUtils.resizeAllColumns(table, DataLayer.DEFAULT_COLUMN_WIDTH);
			int imageColumnPos = columnHeaderLayer.getSelectionLayer().getColumnPositionByIndex(IMAGE_COLUMN_INDEX);
			if (imageColumnPos > -1) {
				NatTableUtils.resizeImageColumn(table, natTableSelectionProvider.getRowDataProvider(), eventList, imageColumnPos, imageBounds, 1);
			} else {
				NatTableUtils.resizeAllRows(table);
			}
		});

		item = new ToolItem(toolbar, SWT.CHECK);
		item.setImage(IconManager.getIconImage("table_groupby.png"));
		item.setToolTipText("Enable Group By Column");
		item.addListener(SWT.Selection, e -> {
			if (table != null && !table.isDisposed() ) {
				table.dispose();

				createNatTable(((ToolItem) e.widget).getSelection());
				setInput(true);
				table.getParent().layout();
			}
		});
	}

	private void createTreeContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> {
			AbstractSiloCommand.setActiveSilo(silo);
			AbstractSiloCommand.setActiveSiloGroup(dataGroup);
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
			final int rowCount = accessor.getRowCount(dataGroup);

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
				registerDisplayConverters(accessor, dataGroup, table.getConfigRegistry());
				table.refresh();
			}
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		} finally {
			eventList.getReadWriteLock().writeLock().unlock();
		}

		// Update rows for Thumbnailer.
		thumbnailViewer.setInput(eventList);
	}

	private void loadGroup(final String path) throws IOException {
		Display.getDefault().syncExec(() -> {
			// Clear Current Selections.
			thumbnailViewer.setSelection(new StructuredSelection());
			natTableSelectionProvider.setSelection(new StructuredSelection());
			// Clear List.
			eventList.clear();
			dataGroup = path;
			setInput(true);
		});
	};

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
							ENTITY row = accessor.getRow(dataGroup, (int) o);
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
							ENTITY row = accessor.getRow(dataGroup, (int) o);
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
			if (event.source instanceof SiloStructure) {
				final SiloStructure struct = (SiloStructure)event.source;
				if (!struct.getSilo().equals(silo)) return;
				refreshCurrentSilo(struct, event);
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

	private void refreshCurrentSilo(SiloStructure struct, ModelEvent event) {
		JobUtils.runBackgroundJob(monitor -> {
			Display.getDefault().asyncExec(() -> {
				setDirty(event.status == 0); // Status 1 means the silo was saved.
				treeViewer.refresh();
				String structGroup = struct.isDataset() ? struct.getParent().getFullName() : struct.getFullName();
				if (structGroup.equals(dataGroup)) setInput(true);
			});
		}, SiloEditor.this.toString(), null, 1000);
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

	private DragSourceListener createDragListener() {
		return new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(natTableSelectionProvider.getSelection());
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				if (event.detail == DND.DROP_MOVE) {
					try {
						List<Integer> rowsList = natTableSelectionProvider.getCurrentListselection();
						int[] rows = new int[rowsList.size()];
						for (int i = 0; i < rows.length; i++) {
							rows[i] = rowsList.get(i);
						}
						accessor.removeRows(dataGroup, rows);
					} catch (SiloException e) {
						EclipseLog.error(e.getMessage(), e, Activator.getDefault());
					}
					selectionProvider.setSelection(new StructuredSelection(natTableSelectionProvider.getSelection()));
					setInput(false);
					setDirty(true);
				}
				toggleDragSupport(false);
			}
		};
	}

	private DropTargetListener createDropListener() {
		return new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				List<ENTITY> selections = getSelectedEntities(LocalSelectionTransfer.getTransfer().getSelection());
				addEntitySelection(selections);
			}
			@Override
			public void dragEnter(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
					List<ENTITY> selections = getSelectedEntities(LocalSelectionTransfer.getTransfer().getSelection());
					if (!selections.isEmpty()) {
						// Only allow a drop if it's from the correct ProtocolClass.
						ProtocolClass pClass = (ProtocolClass) selections.get(0).getAdapter(ProtocolClass.class);
						ProtocolClass currentPClass = silo.getProtocolClass();
						if (currentPClass.equals(pClass)) {
							// It's from the same ProtocolClass, check if it has new selected items.
							boolean isNewSelection = false;
							try {
								Iterator<ENTITY> iter = accessor.getRowIterator(dataGroup);
								while (iter.hasNext()) {
									ENTITY next = iter.next();
									if (!selections.contains(next)) {
										// It has new items, allow drop.
										isNewSelection = true;
										break;
									}
								}
							} catch (SiloException e) {
								EclipseLog.error(e.getMessage(), e, Activator.getDefault());
							}
							if (isNewSelection) event.detail = event.operations;
						}
					}
				}
				event.detail = DND.DROP_NONE;
			}
		};
	}

	private void toggleDragSupport(boolean enabled) {
		disposeExistingDragSupport();
		thumbnailViewer.getThumbnail().setSelectionEnabled(!enabled);;
		if (enabled) {
			thumbnailViewer.addDragSupport(DND.DROP_MOVE, new Transfer[] { LocalSelectionTransfer.getTransfer() },
					createDragListener());
		}
	}
	private void disposeExistingDragSupport() {
		Object o = thumbnailViewer.getThumbnail().getData(DND.DRAG_SOURCE_KEY);
		if (o != null) {
			((DragSource) o).dispose();;
		}
	}

	private void initializeKeyListeners() {
		keyListeners = new Listener[2];
		keyListeners[0] = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.SHIFT) {
					if (!thumbnailViewer.getThumbnail().isDragging()) {
						toggleDragSupport(true);
					}
				}
			}
		};
		keyListeners[1] = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.SHIFT) {
					toggleDragSupport(false);
				}
			}
		};
		Display.getDefault().addFilter(SWT.KeyDown, keyListeners[0]);
		Display.getDefault().addFilter(SWT.KeyUp, keyListeners[1]);
	}

	/*
	 * Private Classes
	 */

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
							ENTITY row = accessor.getRow(dataGroup, rowIndex);
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
							ENTITY row = accessor.getRow(dataGroup, rowIndex);
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
							ENTITY row = accessor.getRow(dataGroup, rowIndex);
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