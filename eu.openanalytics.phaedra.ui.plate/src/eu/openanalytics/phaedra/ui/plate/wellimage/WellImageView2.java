package eu.openanalytics.phaedra.ui.plate.wellimage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import eu.openanalytics.phaedra.base.imaging.overlay.ImageOverlayFactory;
import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.misc.ContextHelper;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingMode;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.ui.wellimage.util.JP2KImageCanvas;
import eu.openanalytics.phaedra.ui.wellimage.util.JP2KImageCanvas.JP2KImageCanvasListener;
import eu.openanalytics.phaedra.ui.wellimage.util.LinkedImageManager;

public class WellImageView2 extends DecoratedView {

	public static final String SETTINGS_ = "SETTINGS_";
	public static final String CHANNELS = "CHANNELS";
	public static final String SCALE = "SCALE";
	public static final String OFFSET_X = "OFFSET_X";
	public static final String OFFSET_Y = "OFFSET_Y";

	private BreadcrumbViewer breadcrumb;

	private ImageControlPanel controlPanel;
	private JP2KImageCanvas imageCanvas;
	private JP2KOverlay[] overlays;

	private int currentMouseMode;

	private ISelectionListener selectionListener;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(parent);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		controlPanel = new ImageControlPanel(parent, SWT.BORDER, false, false);
		controlPanel.addImageControlListener(new ImageControlListener(){
			@Override
			public void componentToggled(int component, boolean state) {
				imageCanvas.changeChannels(controlPanel.getButtonStates());
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(controlPanel);

		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(2, 2).applyTo(container);

		imageCanvas = new JP2KImageCanvas(container, SWT.NONE);
		JP2KImageCanvasListener imageCanvasListener = new JP2KImageCanvasListener() {
			@Override
			public void onFileChange() {
				controlPanel.setImage(imageCanvas.getCurrentWell());
				updateViewTitle();
			}
			@Override
			public void onOffsetChange(int x, int y) {
				if (overlays != null) {
					for (JP2KOverlay overlay: overlays) overlay.setOffset(new Point(x,y));
				}
			}
			@Override
			public void onScaleChange(float newScale) {
				if (overlays != null) {
					for (JP2KOverlay overlay: overlays) overlay.setScale(newScale);
				}
				updateViewTitle();
			}
		};
		imageCanvas.addListener(imageCanvasListener);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(imageCanvas);

		currentMouseMode = 0;

		overlays = new JP2KOverlay[2];
		overlays[0] = (JP2KOverlay)ImageOverlayFactory.create(imageCanvas, "subwell.overlay");
		overlays[1] = (JP2KOverlay)ImageOverlayFactory.create(imageCanvas, "imagethumb.overlay");
		imageCanvas.setOverlays(overlays);

		// This must occur after overlays have been created. Otherwise the imageCanvasListener will not sync the overlays.
		LinkedImageManager.getInstance().addCanvas(imageCanvas);

		selectionListener = (part, selection) -> {
			Well well = SelectionUtils.getFirstObject(selection, Well.class);
			if (well != null) {
				breadcrumb.setInput(well);
				breadcrumb.getControl().getParent().layout();

				imageCanvas.loadImage(well);
				updateViewTitle();
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		for (JP2KOverlay overlay: overlays) {
			overlay.setCurrentMouseMode(currentMouseMode);
			ISelectionListener[] listeners = overlay.getSelectionListeners();
			if (listeners != null) {
				for (ISelectionListener l: listeners) getSite().getPage().addSelectionListener(l);
			}
			if (overlay.getSelectionProvider() != null) getSite().setSelectionProvider(overlay.getSelectionProvider());
		}

		createToolbar();

		addDecorator(new SettingsDecorator(this::getProperties, this::setProperties));
		addDecorator(new CopyableDecorator());

		// Add support for pinning.
		List<ISelectionListener> listeners = new ArrayList<>();
		for (JP2KOverlay o: overlays) {
			for (ISelectionListener l: o.getSelectionListeners()) listeners.add(l);
		}
		listeners.add(selectionListener);
		addDecorator(new SelectionHandlingDecorator(listeners.toArray(new ISelectionListener[0])) {
			@Override
			protected void handleModeChange(SelectionHandlingMode newMode) {
				super.handleModeChange(newMode);
				LinkedImageManager.getInstance().removeCanvas(imageCanvas);
				if (newMode == SelectionHandlingMode.SEL_HILITE) {
					LinkedImageManager.getInstance().addCanvas(imageCanvas);
				}
			}
		});
		initDecorators(parent, imageCanvas);

		// Try to obtain an initial selection.
		SelectionUtils.triggerActiveSelection(selectionListener);

		ContextHelper.attachContext(imageCanvas, CopyItems.COPY_PASTE_CONTEXT_ID);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,"org.eclipse.datatools.connectivity.ui.viewWellImage");
	}

	private Properties getProperties() {
		Properties properties = new Properties();

		for (JP2KOverlay overlay : overlays) {
			properties.addProperty(SETTINGS_ + overlay.getClass().getSimpleName(), overlay.createSettingsMap());
		}
		properties.addProperty(CHANNELS, controlPanel.getButtonStates());
		properties.addProperty(SCALE, imageCanvas.getCurrentScale());
		properties.addProperty(OFFSET_X, imageCanvas.getImageOffset().x);
		properties.addProperty(OFFSET_Y, imageCanvas.getImageOffset().y);

		return properties;
	}

	@SuppressWarnings("unchecked")
	private void setProperties(Properties properties) {
		controlPanel.setButtonStates(properties.getProperty(CHANNELS, controlPanel.getButtonStates()));
		imageCanvas.changeScaleAndImageOffset(
				properties.getProperty(SCALE, imageCanvas.getCurrentScale())
				, properties.getProperty(OFFSET_X, imageCanvas.getImageOffset().x)
				, properties.getProperty(OFFSET_Y, imageCanvas.getImageOffset().y)
		);
		for (JP2KOverlay overlay : overlays) {
			overlay.applySettingsMap((Map<String, Object>) properties.getProperty(SETTINGS_ + overlay.getClass().getSimpleName()));
		}
	}

	@Override
	public void setFocus() {
		imageCanvas.setFocus();
	}

	@Override
	public void dispose() {
		LinkedImageManager.getInstance().removeCanvas(imageCanvas);

		for (JP2KOverlay overlay: overlays) {
			ISelectionListener[] listeners = overlay.getSelectionListeners();
			if (listeners != null) {
				for (ISelectionListener l: listeners) getSite().getPage().removeSelectionListener(l);
			}
		}
		getSite().getPage().removeSelectionListener(selectionListener);
		super.dispose();
	}

	private void updateViewTitle() {
		if (imageCanvas.getCurrentWell() == null) return;
		String coord = NumberUtils.getWellCoordinate(imageCanvas.getCurrentWell().getRow(), imageCanvas.getCurrentWell().getColumn());
		int pct = (int)(imageCanvas.getCurrentScale()*100);
		setPartName("Well Image: " + coord + " (" + pct + "%)");
		Point imageSize = imageCanvas.getCurrentImageSize();
		setTitleToolTip(getPartName() + "\nImage width: " + imageSize.x + "px\nImage height: " + imageSize.y + "px");
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		Action newViewAction = new Action("New Image View", IconManager.getIconDescriptor("image.png")){
			@Override
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				String secId = UUID.randomUUID().toString();
				try {
					WellImageView2 view = (WellImageView2)page.showView(WellImageView2.class.getName(), secId, IWorkbenchPage.VIEW_ACTIVATE);
					if (imageCanvas.getCurrentWell() != null) {
						view.selectionListener.selectionChanged(WellImageView2.this, new StructuredSelection(imageCanvas.getCurrentWell()));
					}
				} catch (PartInitException e) {}
			}
		};
		manager.add(newViewAction);

		Action settingsAction = new Action("Show Image Settings", IconManager.getIconDescriptor("palette.png")){
			@Override
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					ImageSettingsView view = (ImageSettingsView)page.showView(ImageSettingsView.class.getName());
					view.loadWell(imageCanvas.getCurrentWell());
				} catch (PartInitException e) {}
			}
		};
		manager.add(settingsAction);

		manager.add(new Separator());
		for (JP2KOverlay overlay: overlays) overlay.createContextMenu(manager);

		imageCanvas.contributeContextMenu(manager);

		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		super.fillContextMenu(manager);
	}

	private void createToolbar() {
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				final ToolItem mouseModeButton = new ToolItem(parent, SWT.PUSH);
				mouseModeButton.setImage(getMouseModeIcon());
				mouseModeButton.setToolTipText("Toggle selection / panning");
				mouseModeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						currentMouseMode = 1 - currentMouseMode;
						mouseModeButton.setImage(getMouseModeIcon());
						for (JP2KOverlay overlay: overlays) overlay.setCurrentMouseMode(currentMouseMode);
						imageCanvas.setDraggingEnabled(currentMouseMode == 0);
					}
				});
				for (JP2KOverlay overlay: overlays) overlay.createButtons(parent);
			}
			@Override
			public boolean isDynamic() {
				return true;
			}
		};
		mgr.add(contributionItem);
	}

	private Image getMouseModeIcon() {
		Image img = (currentMouseMode == 0) ? IconManager.getIconImage("panning.png") : IconManager.getIconImage("select_lasso.png");
		return img;
	}

}