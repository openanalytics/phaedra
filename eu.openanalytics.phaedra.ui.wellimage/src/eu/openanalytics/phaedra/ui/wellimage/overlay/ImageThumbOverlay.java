package eu.openanalytics.phaedra.ui.wellimage.overlay;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.wellimage.Activator;
import eu.openanalytics.phaedra.ui.wellimage.preferences.Prefs;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.ui.wellimage.util.JP2KImageCanvas;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class ImageThumbOverlay extends JP2KOverlay {

	private static final String ENABLED = "ENABLED";
	private static final String CURRENT_SCALE = "CURRENT_SCALE";

	private Well currentWell;
	private Image currentImage;

	private boolean enabled;

	private boolean[] enabledChannels;
	private float currentScale;

	private IUIEventListener imageSettingsListener;
	private ISelectionListener selectionListener;
	private IPropertyChangeListener prefListener;
	
	private boolean dragging;
	private Cursor dragCursor;

	private MouseListener mouseListener;
	private MouseMoveListener mouseMoveListener;

	private ToolItem thumbBtn;
	private Point size;

	public ImageThumbOverlay() {
		super();

		enabled = false;
		currentScale = Float.NaN;
		dragCursor = Display.getDefault().getSystemCursor(SWT.CURSOR_SIZEALL);

		mouseListener = new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (!enabled) return;
				if (!isOverThumb(e.x, e.y)) return;
				dragging = true;
				getCanvas().setCursor(dragCursor);
			}
			@Override
			public void mouseUp(MouseEvent e) {
				if (!enabled) return;
				if (!isOverThumb(e.x, e.y)) return;
				if (dragging) panImage(e.x, e.y);
				dragging = false;
				getCanvas().setCursor(null);
			}
		};

		mouseMoveListener = new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if (!enabled) return;
				if (dragging) panImage(e.x, e.y);;
			}
		};

		imageSettingsListener = e -> {
			if (e.type == EventType.ImageSettingsChanged) generateImage();
		};
		ProtocolUIService.getInstance().addUIEventListener(imageSettingsListener);
		
		prefListener = (event) -> {
			if (event.getProperty().equals(Prefs.MAX_THUMBNAIL_SIZE)) {
				generateImage();
				getCanvas().redraw();
			}
		};
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(prefListener);
		
		selectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				Well well = SelectionUtils.getFirstObject(selection, Well.class);
				if (well != null) loadWell(well);
			}
		};

		SelectionUtils.triggerActiveEditorSelection(selectionListener);
		if (currentWell == null) SelectionUtils.triggerActiveSelection(selectionListener);
	}

	@Override
	public boolean overridesMouseEvents(int x, int y) {
		if (!enabled) return false;
		return isOverThumb(x, y);
	}

	@Override
	public ISelectionListener[] getSelectionListeners() {
		return new ISelectionListener[]{selectionListener};
	}

	@Override
	public MouseListener getMouseListener() {
		return mouseListener;
	}

	@Override
	public MouseMoveListener getMouseMoveListener() {
		return mouseMoveListener;
	}

	@Override
	public void createButtons(ToolBar parent) {
		thumbBtn = new ToolItem(parent, SWT.CHECK);
		thumbBtn.setImage(IconManager.getIconImage("image_thumb.png"));
		thumbBtn.setToolTipText("Show Thumbnail");
		thumbBtn.setSelection(enabled);
		thumbBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enabled = !enabled;
				if (enabled) generateImage();
				getCanvas().redraw();
			}
		});
	}

	@Override
	public void createContextMenu(IMenuManager manager) {
		if (!enabled) return;
		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem menuItem = new MenuItem(menu, SWT.PUSH, index);
				menuItem.setImage(IconManager.getIconImage("palette_thumb.png"));
				menuItem.setText("Configure Thumbnail...");
				menuItem.addListener(SWT.Selection, e -> { new ConfigDialog(Display.getCurrent().getActiveShell()).open(); });
			}
		};
		manager.add(contributionItem);
	}
	
	@Override
	public void render(GC gc) {
		if (!enabled) return;
		if (currentImage == null || currentImage.isDisposed()) return;
		if (!getCanvas().getSize().equals(size)) {
			generateImage();
			getCanvas().redraw();
			return;
		}
		ImageData data = currentImage.getImageData();
		Rectangle visibleArea = getCanvas().getClientArea();

		// Draw the thumbnail.
		int w = data.width;
		int h = data.height;
		int x = visibleArea.width - w;
		int y = 0;
		gc.drawImage(currentImage, x, y);

		// Draw a border around the thumbnail;
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
		gc.drawRectangle(x, y, w, h);

		// Draw the zoom rectangle within the thumbnail.
		float thumbScale = currentScale / getScale();
		Point pt = translate(new Point(0,0));
		x = x - (int)(thumbScale*pt.x);
		y = y - (int)(thumbScale*pt.y);
		w = (int)(visibleArea.width * thumbScale);
		h = (int)(visibleArea.height * thumbScale);

		w = Math.min(w,  data.width);
		h = Math.min(h,  data.height);

		gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		gc.drawRectangle(x, y, w, h);
	}

	@Override
	public void dispose() {
		if (imageSettingsListener != null) ProtocolUIService.getInstance().removeUIEventListener(imageSettingsListener);
		if (prefListener != null) Activator.getDefault().getPreferenceStore().removePropertyChangeListener(prefListener);
		if (currentImage != null && !currentImage.isDisposed()) currentImage.dispose();
		super.dispose();
	}

	@Override
	public void applySettingsMap(Map<String, Object> settingsMap) {
		if (settingsMap == null) return;
		for (Entry<String, Object> setting : settingsMap.entrySet()) {
			if (setting.getKey().equals(ENABLED)) enabled = (boolean) setting.getValue();
			if (setting.getKey().equals(CURRENT_SCALE)) currentScale = (float) setting.getValue();
		}
		if (thumbBtn != null) thumbBtn.setSelection(enabled);
		getCanvas().redraw();
	}

	@Override
	public Map<String, Object> createSettingsMap() {
		Map<String, Object> settingsMap = new HashMap<>();
		settingsMap.put(ENABLED, enabled);
		settingsMap.put(CURRENT_SCALE, currentScale);
		return settingsMap;
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private void loadWell(Well well) {
		if (well.equals(currentWell)) return;
		if (!PlateUtils.isSameProtocolClass(well, currentWell)) {
			ImageSettings settings = ImageSettingsService.getInstance().getCurrentSettings(well);
			enabledChannels = new boolean[settings.getImageChannels().size()];
			for (int i = 0; i < enabledChannels.length; i++) enabledChannels[i] = settings.getImageChannels().get(i).isShowInPlateView();
		}
		currentWell = well;
		// Fix: make sure the canvas has the correct ImageProvider open. The overlay's listener may be triggered before the canvas' listener.
		((JP2KImageCanvas)getCanvas()).loadImage(well);
		generateImage();
	}

	private void generateImage() {
		if (!enabled || currentWell == null) return;
		if (currentImage != null && !currentImage.isDisposed()) currentImage.dispose();
		try {
			Point imSize = ImageRenderService.getInstance().getWellImageSize(currentWell, 1.0f);
			int w = imSize.x;
			int h = imSize.y;

			int maxThumbnailSize = Activator.getDefault().getPreferenceStore().getInt(Prefs.MAX_THUMBNAIL_SIZE);
			boolean recalcScale = Float.isNaN(currentScale) || (currentScale * w) > maxThumbnailSize || (currentScale * h) > maxThumbnailSize;
			
			if (recalcScale) {
				currentScale = 1f/8f;
				if (getCanvas() != null) {
					int maxX = Math.min(getCanvas().getClientArea().width / 2, maxThumbnailSize);
					int maxY = Math.min(getCanvas().getClientArea().height / 2, maxThumbnailSize);
					while (currentScale * w > maxX) currentScale /= 2;
					while (currentScale * h > maxY) currentScale /= 2;
				}
			}
			
			int scaledW = (int) (currentScale * w);
			int scaledH = (int) (currentScale * h);
			ImageData data = ImageRenderService.getInstance().getWellImageData(currentWell, scaledW, scaledH, enabledChannels);
			currentImage = new Image(null, data);
			size = getCanvas().getSize();
		} catch (IOException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
	}

	private void panImage(int x, int y) {
		if (!isOverThumb(x, y)) return;

		ImageData data = currentImage.getImageData();
		Rectangle visibleArea = getCanvas().getClientArea();
		int minX = visibleArea.width - data.width;
		int minY = 0;

		float thumbScale = currentScale;
		int newX = (int)((x - minX) / thumbScale) - (int)(visibleArea.width/(2*getScale()));
		int newY = (int)((y - minY) / thumbScale) - (int)(visibleArea.height/(2*getScale()));

		JP2KImageCanvas canvas = ((JP2KImageCanvas)getCanvas());
		canvas.changeImageOffset(newX, newY);
	}

	private boolean isOverThumb(int x, int y) {
		if (currentImage == null || currentImage.isDisposed()) return false;
		ImageData data = currentImage.getImageData();
		Rectangle visibleArea = getCanvas().getClientArea();

		int minX = visibleArea.width - data.width;
		int minY = 0;
		int maxX = minX + data.width;
		int maxY = minY + data.height;
		if (x < minX || x > maxX) return false;
		if (y < minY || y > maxY) return false;
		return true;
	}

	private class ConfigDialog extends TitleAreaDialog {

		private boolean[] newEnabledChannels;
		private ImageControlPanel controlPanel;

		public ConfigDialog(Shell parentShell) {
			super(parentShell);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Configure Thumbnail");
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);
			Composite container = new Composite(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(container);

			newEnabledChannels = Arrays.copyOf(enabledChannels, enabledChannels.length);
			ProtocolClass pClass = (ProtocolClass) currentWell.getAdapter(ProtocolClass.class);
			List<ImageChannel> channels = pClass.getImageSettings().getImageChannels();

			Group channelGroup = new Group(container, SWT.NONE);
			channelGroup.setText("Image Channels");
			GridDataFactory.fillDefaults().grab(true, false).applyTo(channelGroup);
			GridLayoutFactory.fillDefaults().numColumns(channels.size()).margins(5, 5).applyTo(channelGroup);

			controlPanel = new ImageControlPanel(channelGroup, SWT.NONE, true, false);
			controlPanel.addImageControlListener(new ImageControlListener() {
				@Override
				public void componentToggled(int component, boolean state) {
					newEnabledChannels[component] = state;
				}
			});
			controlPanel.setImage(currentWell);
			controlPanel.setButtonStates(newEnabledChannels);
			controlPanel.setCurrentScale(currentScale);
			
			setTitle("Configure Thumbnail");
			setMessage("Select the image channels to display in the thumbnail.");
			return area;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			createButton(parent, IDialogConstants.OPEN_ID, "Apply", false);
		}

		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId == IDialogConstants.OPEN_ID) applyPressed();
			else super.buttonPressed(buttonId);
		}

		@Override
		protected void okPressed() {
			applyPressed();
			super.okPressed();
		}
		
		private void applyPressed() {
			System.arraycopy(newEnabledChannels, 0, enabledChannels, 0, newEnabledChannels.length);
			currentScale = controlPanel.getCurrentScale();
			generateImage();
			getCanvas().redraw();
		}
	}
}
