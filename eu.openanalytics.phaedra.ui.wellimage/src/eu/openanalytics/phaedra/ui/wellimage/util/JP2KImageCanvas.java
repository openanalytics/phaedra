package eu.openanalytics.phaedra.ui.wellimage.util;

import java.util.Arrays;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;

import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.wellimage.Activator;
import eu.openanalytics.phaedra.ui.wellimage.canvas.CanvasState;
import eu.openanalytics.phaedra.ui.wellimage.canvas.ICanvasRenderer;
import eu.openanalytics.phaedra.ui.wellimage.canvas.IncrementalCanvasRenderer;
import eu.openanalytics.phaedra.ui.wellimage.preferences.Prefs;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

/**
 * A Canvas that renders a region of a Well's image.
 *
 * By default, all channels are rendered, an panning (by dragging) is enabled.
 */
public class JP2KImageCanvas extends Canvas {

	private static final double ARROW_KEY_MOVEMENT = 0.01;

	private CanvasState canvasState;
	private JP2KOverlay[] overlays;
	
	// Dragging and panning
	private boolean draggingEnabled;
	private boolean dragging;
	private Cursor dragCursor;
	private Point previousDragPoint;

	// Scrolling
	private int lastYScrollBarInteration;
	private boolean isYScrollBarCorrection;
	private int lastScrollBarPosition;
	
	// Listeners
	private ListenerManager listenerManager;

	public JP2KImageCanvas(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.H_SCROLL | SWT.V_SCROLL);

		canvasState = new CanvasState();
		draggingEnabled = true;
		dragCursor = Display.getDefault().getSystemCursor(SWT.CURSOR_SIZEALL);
		
		listenerManager = new ListenerManager();
		listenerManager.onOffsetChange(canvasState.getOffset().x, canvasState.getOffset().y);

		IUIEventListener imageSettingsListener = event -> {
			if (event.type == EventType.ImageSettingsChanged && canvasState.getWell() != null) {
				if (!PlateUtils.isSameProtocolClass(canvasState.getWell(), ProtocolUIService.getInstance().getCurrentProtocolClass())) return;
				canvasState.setForceChange(true);
				redraw();
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(imageSettingsListener);

		ICanvasRenderer canvasRenderer = new IncrementalCanvasRenderer();
		canvasRenderer.initialize(() -> Display.getDefault().asyncExec(() -> redraw()));
		addPaintListener(e -> canvasRenderer.drawImage(e.gc, getClientArea(), canvasState));
		
		getHorizontalBar().addListener(SWT.Selection, e -> {
			int offsetX = (int) (getHorizontalBar().getSelection() / canvasState.getScale());
			changeImageOffset(offsetX, canvasState.getOffset().y);
		});
		getVerticalBar().addListener(SWT.Selection, e -> {
			isYScrollBarCorrection = e.detail == SWT.NONE && lastYScrollBarInteration == e.detail;
			lastYScrollBarInteration = e.detail;
			if (!isYScrollBarCorrection) {
				int offsetY = (int) (getVerticalBar().getSelection() / canvasState.getScale());
				changeImageOffset(canvasState.getOffset().x, offsetY);
				lastScrollBarPosition = getVerticalBar().getSelection();
			} else {
				getVerticalBar().setSelection(lastScrollBarPosition);
			}
		});

		addListener(SWT.Resize, e -> {
			recalculateScrollbars();
			redraw();
		});

		addListener(SWT.Dispose, e -> {
			if (canvasRenderer != null) canvasRenderer.dispose();
			ProtocolUIService.getInstance().removeUIEventListener(imageSettingsListener);
		});

		addMouseWheelListener(e -> changeScale(e.count, e.x, e.y));

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (overlays != null) {
					for (JP2KOverlay overlay: overlays) if (overlay.overridesMouseEvents(e.x, e.y)) return;
				}
				if (!draggingEnabled) return;
				if (e.button != 1) return;
				dragging = true;
				previousDragPoint = new Point(e.x, e.y);
				setCursor(dragCursor);
			}
			@Override
			public void mouseUp(MouseEvent e) {
				if (overlays != null) {
					for (JP2KOverlay overlay: overlays) if (overlay.overridesMouseEvents(e.x, e.y)) return;
				}
				if (!draggingEnabled) return;
				if (e.button != 1) return;
				dragging = false;
				previousDragPoint = null;
				setCursor(null);
			}
		});

		addMouseMoveListener(e -> {
			if (overlays != null) {
				for (JP2KOverlay overlay: overlays) if (overlay.overridesMouseEvents(e.x, e.y)) return;
			}
			if (draggingEnabled && dragging) panImage(e.x, e.y);
		});

		addListener(SWT.KeyDown, e -> {
			switch (e.keyCode) {
			case SWT.ARROW_LEFT:
				changeImageOffset((int) (canvasState.getOffset().x - (canvasState.getFullImageSize().x * ARROW_KEY_MOVEMENT)), canvasState.getOffset().y);
				break;
			case SWT.ARROW_RIGHT:
				changeImageOffset((int) (canvasState.getOffset().x + (canvasState.getFullImageSize().x * ARROW_KEY_MOVEMENT)), canvasState.getOffset().y);
				break;
			case SWT.ARROW_UP:
				changeImageOffset(canvasState.getOffset().x, (int) (canvasState.getOffset().y - (canvasState.getFullImageSize().y * ARROW_KEY_MOVEMENT)));
				break;
			case SWT.ARROW_DOWN:
				changeImageOffset(canvasState.getOffset().x, (int) (canvasState.getOffset().y + (canvasState.getFullImageSize().y * ARROW_KEY_MOVEMENT)));
				break;
			}
		});
	}

	public void loadImage(Well well) {
		Well currentWell = canvasState.getWell();
		if (well == null || well.equals(currentWell)) return;
		
		try {
			if (!well.getPlate().isImageAvailable()) {
				// A well without image is selected: show a blank screen.
				canvasState.clearState();
				canvasState.setWell(well, new Point(0, 0));
			} else if (currentWell == null || !well.getPlate().equals(currentWell.getPlate())) {
				// A well from a different plate is selected
				canvasState.clearState();
				canvasState.setWell(well, ImageRenderService.getInstance().getWellImageSize(well, 1.0f));
				listenerManager.onFileChange();
				
				boolean isSamePClass = PlateUtils.isSameProtocolClass(well, currentWell);
				boolean isRestoredView = (currentWell == null && canvasState.getScale() != 1.0f);
				if (!isSamePClass && !isRestoredView && !LinkedImageManager.getInstance().isLinked(this)) {
					ImageSettings currentSettings = ImageSettingsService.getInstance().getCurrentSettings(well);
					canvasState.setChannels(ProtocolUtils.getEnabledChannels(currentSettings, true));
					changeScale(1f / currentSettings.getZoomRatio());
				}
			} else {
				// A different well from the same plate is selected
				canvasState.setWell(well, ImageRenderService.getInstance().getWellImageSize(well, 1.0f));
			}
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					"Image Error", "The JP2K file could not be opened: " + e.getMessage());
		}

		// The new image may have a size different from the previous. Verify the offsets.
		checkOffsetLimits();
		listenerManager.onOffsetChange(canvasState.getOffset().x, canvasState.getOffset().y);
		recalculateScrollbars();
		redraw();
	}

	public void setOverlays(JP2KOverlay[] overlays) {
		this.overlays = overlays;
	}

	public void setDraggingEnabled(boolean enabled) {
		this.draggingEnabled = enabled;
	}
	
	public float getCurrentScale() {
		return canvasState.getScale();
	}

	public Point getImageOffset() {
		return canvasState.getOffset();
	}

	public Well getCurrentWell() {
		return canvasState.getWell();
	}

	public Point getCurrentImageSize() {
		return canvasState.getFullImageSize();
	}

	public void changeScale(float newScale) {
		if (canvasState.getScale() == newScale) return;
		canvasState.setScale(newScale);
		listenerManager.onScaleChange(canvasState.getScale());
		recalculateScrollbars();
		redraw();
	}

	public void changeImageOffset(int x, int y) {
		int oldX = canvasState.getOffset().x;
		int oldY = canvasState.getOffset().y;
		if (oldX == x && oldY == y) return;
		canvasState.setOffset(new Point(x, y));
		checkOffsetLimits();
		// Prevents endless loop if new offset is outside legal range.
		if (canvasState.getOffset().x == oldX && canvasState.getOffset().y == oldY) return;
		listenerManager.onOffsetChange(canvasState.getOffset().x, canvasState.getOffset().y);
		recalculateScrollbars();
		redraw();
	}

	public void changeScaleAndImageOffset(float newScale, int x, int y) {
		canvasState.setScale(newScale);
		canvasState.setOffset(new Point(x, y));
		checkOffsetLimits();
		listenerManager.onScaleChange(canvasState.getScale());
		listenerManager.onOffsetChange(canvasState.getOffset().x, canvasState.getOffset().y);
		recalculateScrollbars();
		redraw();
	}

	public void changeChannels(boolean[] channels) {
		canvasState.setChannels(Arrays.copyOf(channels, channels.length));
		redraw();
	}
	
	public void contributeContextMenu(IMenuManager manager) {
		Action action = new Action("Save Image to File", IconManager.getIconDescriptor("disk.png")){
			@Override
			public void run() {
				if (canvasState.getWell() == null) return;

				String wellPos = NumberUtils.getWellCoordinate(canvasState.getWell().getRow(), canvasState.getWell().getColumn());
				String fileName = "Well_" + wellPos + ".png";

				Image img = null;
				try {
					ImageData data = ImageRenderService.getInstance().getWellImageData(canvasState.getWell(), canvasState.getScale(), canvasState.getChannels());
					img = new Image(null, data);
					new SaveImageCmd().execute(fileName, img, null);
				} catch (Exception ex) {
					MessageDialog.openError(getShell(), "Save Failed", "Failed to save the image: " + ex.getMessage());
				} finally {
					img.dispose();
				}
			}
		};
		manager.add(action);

		manager.add(new Separator());

		action = new Action("Zoom In", IconManager.getIconDescriptor("magnifier_zoom_in.png")){
			@Override
			public void run() {
				Rectangle clientArea = getClientArea();
				changeScale(1, clientArea.width/2, clientArea.height/2);
			}
		};
		manager.add(action);

		action = new Action("Zoom Out", IconManager.getIconDescriptor("magnifier_zoom_out.png")){
			@Override
			public void run() {
				Rectangle clientArea = getClientArea();
				changeScale(-1, clientArea.width/2, clientArea.height/2);
			}
		};
		manager.add(action);

		action = new Action("Zoom To Fit", IconManager.getIconDescriptor("arrow_out.png")){
			@Override
			public void run() {
				zoomFit();
			}
		};
		manager.add(action);
	}

	/*
	 * Non-public
	 * **********
	 */

	private void zoomFit() {
		if (canvasState.getWell() == null) return;
		
		Point imageSize = ImageRenderService.getInstance().getWellImageSize(canvasState.getWell(), 1.0f);
		Rectangle clientArea = getClientArea();

		float scale = 1.0f;
		while (imageSize.x > clientArea.width || imageSize.y > clientArea.height) {
			// If image is too big, zoom out
			scale = scale / 2;
			imageSize.x = imageSize.x / 2;
			imageSize.y = imageSize.y / 2;
		}
		while (imageSize.x*2 <= clientArea.width && imageSize.y*2 <= clientArea.height) {
			// If image is too small, zoom in
			scale = scale * 2;
			imageSize.x = imageSize.x * 2;
			imageSize.y = imageSize.y * 2;
		}

		changeScale(scale);
	}

	private void recalculateScrollbars() {
		int scaledImageX = (int)(canvasState.getFullImageSize().x*canvasState.getScale());
		int scaledImageY = (int)(canvasState.getFullImageSize().y*canvasState.getScale());

		ScrollBar hBar = getHorizontalBar();
		ScrollBar vBar = getVerticalBar();
		Rectangle client = getClientArea();
		hBar.setMaximum(scaledImageX);
		vBar.setMaximum(scaledImageY);
		hBar.setThumb(Math.min(scaledImageX, client.width));
		vBar.setThumb(Math.min(scaledImageY, client.height));
		hBar.setPageIncrement(hBar.getThumb());
		vBar.setPageIncrement(vBar.getThumb());
		int xVal = (int)(scaledImageX*((double)canvasState.getOffset().x/canvasState.getFullImageSize().x));
		int yVal = (int)(scaledImageY*((double)canvasState.getOffset().y/canvasState.getFullImageSize().y));
		hBar.setSelection(xVal);
		vBar.setSelection(yVal);
		lastScrollBarPosition = yVal;
	}

	private void changeScale(int direction, int x, int y) {
		float maxScale = 4f;
		float minScale = 1f/64f;

		boolean zoomIn = direction > 0;

		if (zoomIn && canvasState.getScale() >= maxScale) return;
		if (!zoomIn && canvasState.getScale() <= minScale) return;
		
		int centerX = canvasState.getOffset().x + (int)(x / canvasState.getScale());
		int centerY = canvasState.getOffset().y + (int)(y / canvasState.getScale());

		float newScale = canvasState.getScale() / 2;
		if (zoomIn) newScale = canvasState.getScale() * 2;

		newScale = Math.max(newScale, minScale);
		newScale = Math.min(newScale, maxScale);

		int w = (int)(getClientArea().width / newScale);
		int h = (int)(getClientArea().height / newScale);

		changeScaleAndImageOffset(newScale, centerX - (w/2), centerY - (h/2));
		
		boolean moveCursor = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.AUTO_MOVE_CURSOR);
		if (moveCursor && x > 0 && x < getClientArea().width && y > 0 && y < getClientArea().height) {
			// Move the mouse to the position in which the user scrolled.
			int newX = (int) ((centerX - canvasState.getOffset().x) * canvasState.getScale());
			int newY = (int) ((centerY - canvasState.getOffset().y) * canvasState.getScale());
			if (newX < getClientArea().width && newY < getClientArea().height) {
				// Only move the cursor when it would be inside the visible area.
				Point location = toDisplay(newX, newY);
				Display.getCurrent().setCursorLocation(location);
			}
		}
	}

	private void panImage(int x, int y) {
		if (previousDragPoint != null) {
			int offX = x - previousDragPoint.x;
			int offY = y - previousDragPoint.y;
			int scaledOffX = (int)(offX/canvasState.getScale());
			int scaledOffY = (int)(offY/canvasState.getScale());
			if (scaledOffX == 0) {
				if (offX > 0) scaledOffX = 1;
				if (offX < 0) scaledOffX = -1;
			}
			if (scaledOffY == 0) {
				if (offY > 0) scaledOffY = 1;
				if (offY < 0) scaledOffY = -1;
			}
			changeImageOffset(canvasState.getOffset().x - scaledOffX, canvasState.getOffset().y - scaledOffY);
		}
		previousDragPoint = new Point(x, y);
	}

	private void checkOffsetLimits() {
		// Verify offset is within bounds.
		Point currentOffset = canvasState.getOffset();
		Point minOffset = new Point(0, 0);
		Point maxOffset = new Point(
				canvasState.getFullImageSize().x - (int) (getClientArea().width / canvasState.getScale()),
				canvasState.getFullImageSize().y - (int) (getClientArea().height / canvasState.getScale()));
		
		Point newOffset = new Point(
				Math.max(minOffset.x, Math.min(maxOffset.x, currentOffset.x)),
				Math.max(minOffset.y, Math.min(maxOffset.y, currentOffset.y)));
		canvasState.setOffset(newOffset);
	}

	/*
	 * Event management
	 * ****************
	 */

	public void addListener(JP2KImageCanvasListener listener) {
		listenerManager.addListener(listener);
	}

	public void removeListener(JP2KImageCanvasListener listener) {
		listenerManager.removeListener(listener);
	}

	private class ListenerManager extends EventManager implements JP2KImageCanvasListener {

		public void addListener(JP2KImageCanvasListener listener) {
			addListenerObject(listener);
		}

		public void removeListener(JP2KImageCanvasListener listener) {
			removeListenerObject(listener);
		}

		@Override
		public void onFileChange() {
			for (Object o: getListeners()) {
				((JP2KImageCanvasListener)o).onFileChange();
			}
		}

		@Override
		public void onScaleChange(float newScale) {
			for (Object o: getListeners()) {
				((JP2KImageCanvasListener)o).onScaleChange(newScale);
			}
		}

		@Override
		public void onOffsetChange(int x, int y) {
			for (Object o: getListeners()) {
				((JP2KImageCanvasListener)o).onOffsetChange(x, y);
			}
		}
	}

	public static interface JP2KImageCanvasListener {

		public void onFileChange();

		public void onScaleChange(float newScale);

		public void onOffsetChange(int x, int y);
	}

}
