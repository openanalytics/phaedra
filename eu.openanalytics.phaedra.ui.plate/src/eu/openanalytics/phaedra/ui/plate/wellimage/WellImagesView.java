package eu.openanalytics.phaedra.ui.plate.wellimage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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
import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.classification.WellClassificationSupport;
import eu.openanalytics.phaedra.ui.plate.util.WellUtils;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.wellimage.tooltip.WellToolTipLabelProvider;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class WellImagesView extends DecoratedView {

	private ISelectionListener selectionListener;
	private ISelectionListener highlightListener;
	private IUIEventListener imageSettingListener;
	private Listener[] keyListeners;

	private ImageControlPanel controlPanel;
	private ThumbnailViewer thumbnailViewer;

	private ProtocolClass pClass;
	private List<Well> currentWells;

	private Job currentJob;

	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(2,2).applyTo(container);

		this.currentWells = new ArrayList<>();

		controlPanel = new ImageControlPanel(container, SWT.BORDER, true, false);
		// Default scale 1:32.
		controlPanel.setCurrentScale((float)1/32);
		controlPanel.addImageControlListener(new ImageControlListener() {
			@Override
			public void componentToggled(int component, boolean state) {
				thumbnailViewer.getThumbnail().redraw();
			}
			@Override
			public void scaleChanged(float ratio) {
				thumbnailViewer.resizeImages();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(controlPanel);

		thumbnailViewer = new ThumbnailViewer(container);
		thumbnailViewer.setContentProvider(new ArrayContentProvider());
		thumbnailViewer.setLabelProvider(new AbstractThumbnailLabelProvider() {
			@Override
			public IThumbnailCellRenderer createCellRenderer() {
				return new AbstractThumbnailCellRenderer() {
					@Override
					public ImageData getImageData(Object o) {
						if (o instanceof Well) {
							Well well = (Well) o;
							try {
								return ImageRenderService.getInstance().getWellImageData(
										well, controlPanel.getCurrentScale(), controlPanel.getButtonStates());
							} catch (IOException e) {
								// Do nothing.
							}
						}
						return null;
					}
					@Override
					public Rectangle getImageBounds(Object o) {
						if (o instanceof Well) {
							Well well = (Well) o;
							Point wellImageSize = ImageRenderService.getInstance().getWellImageSize(
									well, controlPanel.getCurrentScale());
							return new Rectangle(0, 0, wellImageSize.x, wellImageSize.y);
						}
						return new Rectangle(0, 0, 0, 0);
					}
					@Override
					public boolean isImageReady(Object o) {
						if (o instanceof Well) {
							Well well = (Well) o;
							return ImageRenderService.getInstance().isWellImageCached(
									well, controlPanel.getCurrentScale(), controlPanel.getButtonStates());
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
		thumbnailViewer.setSelectionConfig(ConfigurableStructuredSelection.NO_PARENT);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(thumbnailViewer.getControl());

		// Add tooltips.
		new ThumbnailTooltip(thumbnailViewer, new WellToolTipLabelProvider());

		createJob();
		createListeners();
		initializeKeyListeners();

		addDecorator(new WellClassificationSupport(false, true));
		addDecorator(new SelectionHandlingDecorator(selectionListener, highlightListener));
		addDecorator(new CopyableDecorator());
		initDecorators(parent, thumbnailViewer.getControl());

		SelectionUtils.triggerActiveEditorSelection(selectionListener);

		ContextHelper.attachContext(thumbnailViewer.getControl(), CopyItems.COPY_PASTE_CONTEXT_ID);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewWellImages");
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
				item.addListener(SWT.Selection, e -> {
					Dialog dialog = new ThumbnailViewerConfigDialog(getSite().getShell(), thumbnailViewer);
					dialog.open();
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
		currentJob = new Job("Loading Well Images") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Display.getDefault().asyncExec(() -> {
					if (!thumbnailViewer.getControl().isDisposed()) {
						thumbnailViewer.setInput(currentWells);
						setTitleToolTip(getPartName() + "\nNr. of Items: " + currentWells.size());
					}
				});
				return Status.OK_STATUS;
			}
		};
	}

	private void createListeners() {
		selectionListener = (part, selection) -> {
			if (part == WellImagesView.this) return;

			// Incoming Well Selection.
			List<Well> wells = WellUtils.getWells(selection);
			if (!wells.isEmpty() && !wells.equals(currentWells)) {
				currentJob.cancel();

				checkProtocolClass(wells.get(0));
				currentWells = wells;

				currentJob.schedule(500);
			}
		};

		highlightListener = (part, selection) -> {
			if (part == WellImagesView.this) return;

			List<Well> wells = WellUtils.getWells(selection);
			if (wells.isEmpty()) return;
			thumbnailViewer.setSelection(new StructuredSelection(wells));
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
		keyListeners[0] = event -> {
			if (event.keyCode == SWT.SHIFT && !thumbnailViewer.getThumbnail().isDragging()) {
				toggleDragSupport(true);
			}
		};
		keyListeners[1] = event -> {
			if (event.keyCode == SWT.SHIFT) {
				toggleDragSupport(false);
			}
		};
		Display.getDefault().addFilter(SWT.KeyDown, keyListeners[0]);
		Display.getDefault().addFilter(SWT.KeyUp, keyListeners[1]);
	}

}