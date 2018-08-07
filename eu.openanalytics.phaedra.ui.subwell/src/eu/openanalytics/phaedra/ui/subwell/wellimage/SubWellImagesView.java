package eu.openanalytics.phaedra.ui.subwell.wellimage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.AbstractThumbnailCellRenderer;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.AbstractThumbnailLabelProvider;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.IThumbnailCellRenderer;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.ThumbnailTooltip;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.ThumbnailViewer;
import eu.openanalytics.phaedra.base.ui.thumbnailviewer.ThumbnailViewerConfigDialog;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.misc.ContextHelper;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.model.subwell.util.SubWellUtils;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.subwell.SubWellClassificationSupport;
import eu.openanalytics.phaedra.ui.wellimage.tooltip.SubWellToolTipLabelProvider;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class SubWellImagesView extends DecoratedView {

	private ISelectionListener selectionListener;
	private ISelectionListener highlightListener;
	private IUIEventListener imageSettingListener;
	private Listener[] keyListeners;

	private ImageControlPanel controlPanel;
	private ThumbnailViewer thumbnailViewer;

	private ProtocolClass pClass;
	private Map<SubWellItem, Rectangle> imageSizes;
	private List<Well> currentWells;
	private List<SubWellItem> currentSWItems;
	private float currentScale;

	private Job currentJob;
	private Job resizeJob;

	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(2,2).applyTo(container);

		this.currentWells = new ArrayList<>();
		this.currentSWItems = new ArrayList<>();
		this.imageSizes = new ConcurrentHashMap<>();

		controlPanel = new ImageControlPanel(container, SWT.BORDER, true, false);
		controlPanel.addImageControlListener(new ImageControlListener() {
			@Override
			public void componentToggled(int component, boolean state) {
				thumbnailViewer.getThumbnail().redraw();
			}
			@Override
			public void scaleChanged(float ratio) {
				resizeJob.cancel();
				resizeJob.schedule();
			}
		});
		currentScale = controlPanel.getCurrentScale();
		GridDataFactory.fillDefaults().grab(true, false).applyTo(controlPanel);

		thumbnailViewer = new ThumbnailViewer(container) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			protected List getSelectionFromWidget() {
				// Convert the List<Cell> to a List<SubWellSelection
				List sel = super.getSelectionFromWidget();
				if (!sel.isEmpty()) {
					Map<Well, SubWellSelection> tempMap = new HashMap<>();
					for (Object o : sel) {
						if (o instanceof SubWellItem) {
							SubWellItem cell = (SubWellItem) o;
							Well well = cell.getWell();
							if (!tempMap.containsKey(well)) {
								tempMap.put(well, new SubWellSelection(well, new BitSet()));
							}
							tempMap.get(well).getIndices().set(cell.getIndex());
						}
					}
					sel = new ArrayList<SubWellSelection>();
					sel.addAll(tempMap.values());
				}
				return sel;
			}
			@SuppressWarnings("rawtypes")
			@Override
			protected void setSelectionToWidget(List list, boolean reveal) {
				// Convert the List<SubWellSelection> to List<Cell>
				if (!list.isEmpty()) {
					List<SubWellItem> sel = new ArrayList<>();
					for (Object o : list) {
						if (o instanceof SubWellSelection) {
							SubWellSelection sw = (SubWellSelection) o;
							BitSet indices = sw.getIndices();
							for (int i = indices.nextSetBit(0); i >= 0; i = indices.nextSetBit(i+1)) {
								sel.add(new SubWellItem(sw.getWell(), i));
							}
						}
						if (o instanceof SubWellItem) {
							sel.add((SubWellItem) o);
						}
					}
					super.setSelectionToWidget(sel, reveal);
				}
			}
		};
		thumbnailViewer.setContentProvider(new ArrayContentProvider());
		thumbnailViewer.setLabelProvider(new AbstractThumbnailLabelProvider() {
			@Override
			public IThumbnailCellRenderer createCellRenderer() {
				return new AbstractThumbnailCellRenderer() {
					@Override
					public ImageData getImageData(Object o) {
						if (o instanceof SubWellItem) {
							SubWellItem cell = (SubWellItem) o;
							try {
								return ImageRenderService.getInstance().getSubWellImageData(
										cell.getWell(), cell.getIndex(), currentScale, controlPanel.getButtonStates());
							} catch (IOException e) {
								// Do nothing.
							}
						}
						return null;
					}
					@Override
					public Rectangle getImageBounds(Object o) {
						if (o instanceof SubWellItem) {
							SubWellItem cell = (SubWellItem) o;
							return imageSizes.get(cell);
						}
						return new Rectangle(0, 0, 0, 0);
					}
					@Override
					public boolean isImageReady(Object o) {
						if (o instanceof SubWellItem) {
							SubWellItem cell = (SubWellItem) o;
							return ImageRenderService.getInstance().isSubWellImageCached(
									cell.getWell(), cell.getIndex(), currentScale, controlPanel.getButtonStates());
						}
						return true;
					}
				};
			}
		});
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
		thumbnailViewer.getThumbnail().addKeyListener(controlPanel.getKeyListener());
		thumbnailViewer.getThumbnail().addMouseWheelListener(controlPanel.getMouseWheelListener());
		GridDataFactory.fillDefaults().grab(true,true).applyTo(thumbnailViewer.getControl());

		// Add tooltips.
		new ThumbnailTooltip(thumbnailViewer, new SubWellToolTipLabelProvider());

		createJob();
		createListeners();
		initializeKeyListeners();

		addDecorator(new SubWellClassificationSupport(false, true));
		addDecorator(new SelectionHandlingDecorator(selectionListener, highlightListener));
		addDecorator(new CopyableDecorator());
		initDecorators(parent, thumbnailViewer.getControl());

		// Obtain an initial selection.
		SelectionUtils.triggerActiveSelection(selectionListener);

		ContextHelper.attachContext(thumbnailViewer.getControl(), CopyItems.COPY_PASTE_CONTEXT_ID);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewSubwellImages");
	}

	@Override
	public void setFocus() {
		thumbnailViewer.getThumbnail().setFocus();
	}

	@Override
	public void dispose() {
		Display.getDefault().removeFilter(SWT.KeyDown, keyListeners[0]);
		Display.getDefault().removeFilter(SWT.KeyUp, keyListeners[1]);
		getSite().getPage().removeSelectionListener(selectionListener);
		getSite().getPage().removeSelectionListener(highlightListener);
		ProtocolUIService.getInstance().removeUIEventListener(imageSettingListener);
		super.dispose();
	}

	@Override
	protected void fillToolbar() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mgr = bars.getToolBarManager();

		mgr.add(new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem item = new ToolItem(parent, SWT.PUSH);
				item.setImage(IconManager.getIconImage("settings.gif"));
				item.setToolTipText("Change the settings for the Thumbnail Viewer");
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						Dialog dialog = new ThumbnailViewerConfigDialog(getSite().getShell(), thumbnailViewer);
						dialog.open();
					}
				});
			}
		});

		super.fillToolbar();
	}

	private void toggleDragSupport(boolean enabled) {
		disposeExistingDragSupport();
		thumbnailViewer.getThumbnail().setSelectionEnabled(!enabled);;
		if (enabled) {
			thumbnailViewer.addDragSupport(
					DND.DROP_MOVE
					, new Transfer[] { LocalSelectionTransfer.getTransfer() }
					, createDragListener()
					);
		}
	}

	private void disposeExistingDragSupport() {
		Object o = thumbnailViewer.getThumbnail().getData(DND.DRAG_SOURCE_KEY);
		if (o != null) {
			((DragSource) o).dispose();;
		}
	}

	private DragSourceListener createDragListener() {
		return new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(thumbnailViewer.getSelection());
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				toggleDragSupport(false);
			}
		};
	}

	private void createJob() {
		currentJob = new Job(getPartName() + ": Retrieving Image Sizes") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				float scaleWhenLoad = controlPanel.getCurrentScale();
				Map<SubWellItem, Rectangle> tempMap = new ConcurrentHashMap<>();
				List<SubWellItem> tempItems = new ArrayList<>(currentSWItems);
				List<Well> tempList = new ArrayList<>(currentWells);
				if (tempItems.isEmpty()) {
					monitor.beginTask("Gathering Subwell Items", tempList.size());
					for (Well w : tempList) {
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						int size = SubWellService.getInstance().getNumberOfCells(w);
						monitor.setTaskName("Gathering Subwell Items for Well " + NumberUtils.getWellCoordinate(w.getRow(), w.getColumn()));
						for (int i = 0; i < size; i++) {
							SubWellItem c = new SubWellItem(w, i);
							tempItems.add(c);
							tempMap.put(c, ImageRenderService.getInstance().getSubWellImageBounds(c.getWell(), c.getIndex(), scaleWhenLoad));
						}
						monitor.worked(1);
					}
				} else {
					monitor.beginTask("Images done: " + 0, tempItems.size());
					for (SubWellItem c : tempItems) {
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						tempMap.put(c, ImageRenderService.getInstance().getSubWellImageBounds(c.getWell(), c.getIndex(), scaleWhenLoad));
						monitor.setTaskName("Images done: " + tempMap.size() + "/" + tempItems.size());
						monitor.worked(1);
					}
				}
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						if (!thumbnailViewer.getControl().isDisposed()) {
							imageSizes = tempMap;
							currentSWItems = tempItems;
							currentWells = tempList;
							controlPanel.setCurrentScale(scaleWhenLoad, false);
							thumbnailViewer.setInput(currentSWItems);
							setTitleToolTip(getPartName() + "\nNr. of Items: " + currentSWItems.size());
						}
					}
				});
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		resizeJob = new Job(getPartName() + ": Change Image Scale") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				float newScale = controlPanel.getCurrentScale();
				if (Math.abs(newScale - currentScale) < 0.001) return Status.OK_STATUS;
				monitor.beginTask("Change Image Scale", imageSizes.keySet().size());
				// Since we get the size from our map, it needs to be updated.
				Map<SubWellItem, Rectangle> tempMap = new ConcurrentHashMap<>();
				for (SubWellItem c : imageSizes.keySet()) {
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					tempMap.put(c, ImageRenderService.getInstance().getSubWellImageBounds(c.getWell(), c.getIndex(), newScale));
					monitor.worked(1);
				}
				imageSizes = tempMap;
				currentScale = newScale;
				Display.getDefault().syncExec(() -> {
					if (!thumbnailViewer.getControl().isDisposed()) {
						thumbnailViewer.resizeImages();
					}
				});
				monitor.done();
				return Status.OK_STATUS;
			}
		};
	}

	private void createListeners() {
		selectionListener = (part, selection) -> {
			if (part == SubWellImagesView.this) return;

			// Incoming SubWell Selection.
			List<SubWellItem> swItems = SubWellUtils.getSubWellItems(selection);
			if (!swItems.isEmpty() && !swItems.equals(currentSWItems)) {
				currentJob.cancel();

				checkProtocolClass(swItems.get(0).getWell());

				currentWells.clear();
				currentSWItems = swItems;

				currentJob.schedule(500);
				return;
			} else if (SelectionUtils.getFirstObject(selection, Well.class) != null) {
				// Incoming Well selection.
				List<Well> wells = SelectionUtils.getObjects(selection, Well.class);

				if (!wells.equals(currentWells)) {
					currentJob.cancel();

					checkProtocolClass(wells.get(0));

					currentWells = wells;
					currentSWItems.clear();

					currentJob.schedule(500);
				}
				return;
			}
		};

		highlightListener = (part, selection) -> {
			if (part == SubWellImagesView.this) return;

			List<SubWellItem> swItems = SubWellUtils.getSubWellItems(selection);
			if (swItems.isEmpty()) return;
			thumbnailViewer.setSelection(new StructuredSelection(swItems));
		};

		imageSettingListener = event -> {
			if (event.type == EventType.ImageSettingsChanged) {
				thumbnailViewer.getControl().redraw();
			}
		};

		ProtocolUIService.getInstance().addUIEventListener(imageSettingListener);
		getSite().getPage().addSelectionListener(selectionListener);
		getSite().getPage().addSelectionListener(highlightListener);
		getSite().setSelectionProvider(thumbnailViewer);
	}

	private void checkProtocolClass(Well well) {
		ProtocolClass newPClass = PlateUtils.getProtocolClass(well);
		if (pClass == null || !pClass.equals(newPClass)) {
			pClass = newPClass;
			controlPanel.setImage(pClass);
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

}