package eu.openanalytics.phaedra.ui.wellimage.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.ToolBar;

import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.wellimage.Activator;
import eu.openanalytics.phaedra.ui.wellimage.preferences.Prefs;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

/**
 * A Canvas that renders a region of a Well's image.
 *
 * By default, all channels are rendered, an panning (by dragging) is enabled.
 */
public class JP2KImageCanvas extends Canvas {

	private static final double ARROW_KEY_MOVEMENT = 0.01;

	private JP2KOverlay[] overlays;
	
	private Well currentWell;
	
	private Image currentImage;
	private Point currentImageFullSize;
	private Rectangle currentRenderArea;
	private boolean[] currentImageChannels;

	private float currentScale;
	private Point currentOffset;
	
	// Dragging and panning
	private boolean draggingEnabled;
	private boolean dragging;
	private Cursor dragCursor;
	private Point previousDragPoint;
	private boolean hasPanned;

	// Scrolling
	private int lastYScrollBarInteration;
	private boolean isYScrollBarCorrection;
	private int lastScrollBarPosition;
	
	// Listeners
	private ListenerManager listenerManager;
	private IUIEventListener imageSettingsListener;

	public JP2KImageCanvas(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.H_SCROLL | SWT.V_SCROLL);

		listenerManager = new ListenerManager();

		imageSettingsListener = event -> {
			if (event.type == EventType.ImageSettingsChanged && currentWell != null) {
				updateImageSettings();
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(imageSettingsListener);

		addPaintListener(e -> drawImage(e.gc));

		draggingEnabled = true;
		clearCacheValues();
		currentOffset = new Point(0,0);
		currentImageFullSize = new Point(0,0);
		currentScale = 1.0f;
		dragCursor = Display.getDefault().getSystemCursor(SWT.CURSOR_SIZEALL);
		listenerManager.onOffsetChange(currentOffset.x, currentOffset.y);

		getHorizontalBar().addListener(SWT.Selection, e -> scrollX());
		getVerticalBar().addListener(SWT.Selection, e -> {
			isYScrollBarCorrection = e.detail == SWT.NONE && lastYScrollBarInteration == e.detail;
			lastYScrollBarInteration = e.detail;
			if (!isYScrollBarCorrection) {
				scrollY();
				lastScrollBarPosition = getVerticalBar().getSelection();
			} else {
				getVerticalBar().setSelection(lastScrollBarPosition);
			}
		});

		addListener(SWT.Resize, e -> {
			clearCacheValues();
			updateScrollbars();
		});

		addListener(SWT.Dispose, e -> {
			clearCacheValues();
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
				changeImageOffset((int) (currentOffset.x - (currentImageFullSize.x * ARROW_KEY_MOVEMENT)), currentOffset.y);
				break;
			case SWT.ARROW_RIGHT:
				changeImageOffset((int) (currentOffset.x + (currentImageFullSize.x * ARROW_KEY_MOVEMENT)), currentOffset.y);
				break;
			case SWT.ARROW_UP:
				changeImageOffset(currentOffset.x, (int) (currentOffset.y - (currentImageFullSize.y * ARROW_KEY_MOVEMENT)));
				break;
			case SWT.ARROW_DOWN:
				changeImageOffset(currentOffset.x, (int) (currentOffset.y + (currentImageFullSize.y * ARROW_KEY_MOVEMENT)));
				break;
			}
		});
	}

	public void setOverlays(JP2KOverlay[] overlays) {
		this.overlays = overlays;
	}

	public void setDraggingEnabled(boolean enabled) {
		draggingEnabled = enabled;
	}

	public void loadImage(Well well) {
		if (well.equals(currentWell)) return;
		boolean samePClass = PlateUtils.isSameProtocolClass(well, currentWell);
		boolean restoredView = (currentWell == null && currentScale != 1.0f);

		try {
			if (currentWell == null || !well.getPlate().equals(currentWell.getPlate())) {
				// If a well without image is selected, show a blank screen.
				if (!well.getPlate().isImageAvailable()) {
					redraw();
					currentWell = well;
					return;
				}

				currentWell = well; // currentWell must not be null during onFileChange.
				listenerManager.onFileChange();
			}

			// Load information about the new well image.
			currentWell = well;
			currentImageFullSize = ImageRenderService.getInstance().getWellImageSize(well, 1.0f);
			// If the protocol class changed and the canvas is not linked to another canvas, apply the protocol class' default scale.
			if (!restoredView && !samePClass && !LinkedImageManager.getInstance().isLinked(this)) {
				ImageSettings currentSettings = ImageSettingsService.getInstance().getCurrentSettings(well);
				changeScale(1f/currentSettings.getZoomRatio());
			}
			// The new image may have a size different from the previous. Verify the offsets.
			checkOffsetLimits();
			listenerManager.onOffsetChange(currentOffset.x, currentOffset.y);

		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					"Image Error", "The JP2K file could not be opened: " + e.getMessage());
		}

		clearCacheValues();
		updateScrollbars();
	}

	public void forceRedraw() {
		forceRedraw(false);
	}

	public void forceRedraw(boolean clearCache) {
		if (clearCache) clearCacheValues();
		setPanned();
		super.redraw();
	}

	public void closeImageProvider() {
		//TODO remove
	}

	public void openImageProvider() throws IOException {
		//TODO remove
	}

	public float getCurrentScale() {
		return currentScale;
	}

	public Point getImageOffset() {
		return currentOffset;
	}

	public Well getCurrentWell() {
		return currentWell;
	}

	public Point getCurrentImageSize() {
		return currentImageFullSize;
	}

	// All in one solution for Reporting
	public void changeScaleAndImageOffset(float newScale, int x, int y) {
		currentScale = newScale;
		currentOffset.x = x;
		currentOffset.y = y;
		checkOffsetLimits();
		listenerManager.onScaleChange(currentScale);
		listenerManager.onOffsetChange(currentOffset.x, currentOffset.y);
		updateScrollbars();
		clearCacheValues();
		update();
	}

	public void changeScale(float newScale) {
		if (currentScale == newScale) return;
		currentScale = newScale;
		listenerManager.onScaleChange(currentScale);
		updateScrollbars();
		clearCacheValues();
	}

	public void changeImageOffset(int x, int y) {
		int oldX = currentOffset.x;
		int oldY = currentOffset.y;
		if (oldX == x && oldY == y) return;
		currentOffset.x = x;
		currentOffset.y = y;
		checkOffsetLimits();
		// Prevents endless loop if new offset is outside legal range.
		if (currentOffset.x == oldX && currentOffset.y == oldY) return;
		listenerManager.onOffsetChange(currentOffset.x, currentOffset.y);
		updateScrollbars();
		update();
	}

	public void createButtons(ToolBar parent) {
		// Nothing to add.
	}

	public void contributeContextMenu(IMenuManager manager) {
		Action action = new Action("Save Image to File", IconManager.getIconDescriptor("disk.png")){
			@Override
			public void run() {
				if (currentWell == null) return;

				String wellPos = NumberUtils.getWellCoordinate(currentWell.getRow(), currentWell.getColumn());
				String fileName = "Well_" + wellPos + ".png";

				Image img = null;
				try {
					ImageData data = ImageRenderService.getInstance().getWellImageData(currentWell, currentScale, currentImageChannels);
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

	protected boolean isChannelEnabled(int nr) {
		return true;
	}

	private void updateImageSettings() {
		if (!PlateUtils.isSameProtocolClass(currentWell, ProtocolUIService.getInstance().getCurrentProtocolClass())) return;
		clearCacheValues();
		forceRedraw();
	}

	private void clearCacheValues() {
		hasPanned = false;
		currentRenderArea = new Rectangle(0, 0, 0, 0);
		currentImageChannels = null;
		if (currentImage != null) currentImage.dispose();
		currentImage = null;
	}

	private void zoomFit() {
		if (currentWell == null) return;
		
		Point imageSize = ImageRenderService.getInstance().getWellImageSize(currentWell, 1.0f);
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

	private void setPanned() {
		hasPanned = true;
	}

	private void drawImage(GC gc) {
		if (currentWell == null) return;

		if (!hasPanned && currentImage != null && !currentImage.isDisposed()) {
			gc.drawImage(currentImage, 0, 0);
			return;
		}

		try {
			// Find out which area of the image to render.
			Rectangle clientArea = getClientArea();
			Rectangle targetRenderArea = new Rectangle(currentOffset.x, currentOffset.y,
					(int) Math.ceil(clientArea.width/currentScale),
					(int) Math.ceil(clientArea.height/currentScale));
			
			// If the client area is bigger than the image area, reduce image area a bit.
			if (targetRenderArea.width > currentImageFullSize.x) targetRenderArea.width = currentImageFullSize.x;
			if (targetRenderArea.height > currentImageFullSize.y) targetRenderArea.height = currentImageFullSize.y;

			// If the right edge of the image is reached, go back left a bit.
			int widthNeeded = (int)(currentScale*(currentImageFullSize.x - currentOffset.x));
			int widthAvailable = getClientArea().width;
			if (widthNeeded < widthAvailable && currentOffset.x > 0) {
				currentOffset.x = Math.max(0, currentImageFullSize.x - (int)(widthAvailable/currentScale));
				for (JP2KOverlay o: overlays) o.setOffset(currentOffset);
			}

			Image newImage = new Image(getDisplay(), clientArea.width, clientArea.height);
			
			// Reuse the part of the current image that is still on-screen (if any)
			if (currentImage != null) {
				int deltaX = (int) (currentScale * (currentRenderArea.x - currentOffset.x));
				int deltaY = (int) (currentScale * (currentRenderArea.y - currentOffset.y));
				
				GC imageGC = new GC(newImage);
				imageGC.drawImage(currentImage, deltaX, deltaY);
				imageGC.dispose();

				currentImage.dispose();
			}

			// Determine the channels to render.
			List<ImageChannel> channels = ImageSettingsService.getInstance().getCurrentSettings(currentWell).getImageChannels();
			boolean[] enabledChannels = new boolean[channels.size()];
			IntStream.range(0, channels.size()).forEach(i -> enabledChannels[i] = isChannelEnabled(channels.get(i).getSequence()));

			// Calculate missing parts between current and target area
			for (Slice slice : subtract(currentRenderArea, targetRenderArea)) {
				ImageData data = ImageRenderService.getInstance().getWellImageData(currentWell, currentScale, slice.bounds, enabledChannels);
				Image sliceImage = new Image(null, data);
				GC imageGC = new GC(newImage);
				slice.drawOnGC(imageGC, sliceImage, clientArea);
				imageGC.dispose();
				sliceImage.dispose();
			}
			gc.drawImage(newImage, 0, 0);

			// Update cached values
			currentImage = newImage;
			currentRenderArea = targetRenderArea;

			if (currentImageChannels != null && !Arrays.equals(currentImageChannels, enabledChannels)) {
				clearCacheValues();
				updateScrollbars();
				forceRedraw();
			} else {
				hasPanned = false;
			}
			currentImageChannels = enabledChannels;
			
		} catch (Exception e) {
			EclipseLog.error("Failed to render image", e, Activator.PLUGIN_ID);
		}
	}

	private Collection<Slice> subtract(Rectangle original, Rectangle target) {
		List<Slice> slices = new ArrayList<>();
		// Case 0: currentRect = 0 or the rectangles do not overlap at all.
		if (original.width == 0 && original.height == 0 || !original.intersects(target)) {
			slices.add(new Slice(target) {
				@Override
				public void drawOnGC(GC gc, Image image, Rectangle target) {
					gc.drawImage(image, target.x, target.y);
				}
			});
			return slices;
		}

		int height = original.y - target.y;
		int absHeight = Math.abs(height);
		Rectangle rectangle = new Rectangle(target.x, target.y, target.width, absHeight);

		if (rectangle.height > target.height) {
			rectangle.height = target.height; //Image is panned further than height of visible area
		}

		if (height != 0) {
			// Case 1: currentRect is panned down
			if (height > 0) {
				slices.add(new Slice(rectangle) {
					@Override
					public void drawOnGC(GC gc, Image image, Rectangle target) {
						gc.drawImage(image, target.x, target.y);
					}
				});
				// Case 2: currentRect is panned up
			} else {
				rectangle.y += original.height - absHeight;
				slices.add(new Slice(rectangle) {
					@Override
					public void drawOnGC(GC gc, Image image, Rectangle target) {
						int y = target.height - (int)(bounds.height*currentScale);
						gc.drawImage(image, target.x, y);
					}
				});
			}
		}

		int width = original.x - target.x;
		int absWidth = Math.abs(width);
		rectangle = new Rectangle(target.x, target.y, absWidth, target.height);

		if (rectangle.width > target.width) {
			rectangle.width = target.width; //Image is panned further than width of visible area
		}

		if (width != 0) {
			// Case 3: currentRect is panned down
			if (width > 0) {
				slices.add(new Slice(rectangle) {
					@Override
					public void drawOnGC(GC gc, Image image, Rectangle target) {
						gc.drawImage(image, target.x, target.y);
					}
				});
				// Case 4: currentRect is panned up
			} else {
				rectangle.x += original.width - absWidth;
				slices.add(new Slice(rectangle) {
					@Override
					public void drawOnGC(GC gc, Image image, Rectangle target) {
						int x = target.width - (int)(bounds.width*currentScale);
						gc.drawImage(image, x, target.y);
					}
				});
			}
		}

		return slices;
	}

	private void updateScrollbars() {
		int scaledImageX = (int)(currentImageFullSize.x*currentScale);
		int scaledImageY = (int)(currentImageFullSize.y*currentScale);

		ScrollBar hBar = getHorizontalBar();
		ScrollBar vBar = getVerticalBar();
		Rectangle client = getClientArea();
		hBar.setMaximum(scaledImageX);
		vBar.setMaximum(scaledImageY);
		hBar.setThumb(Math.min(scaledImageX, client.width));
		vBar.setThumb(Math.min(scaledImageY, client.height));
		hBar.setPageIncrement(hBar.getThumb());
		vBar.setPageIncrement(vBar.getThumb());
		int xVal = (int)(scaledImageX*((double)currentOffset.x/currentImageFullSize.x));
		int yVal = (int)(scaledImageY*((double)currentOffset.y/currentImageFullSize.y));
		hBar.setSelection(xVal);
		vBar.setSelection(yVal);
		lastScrollBarPosition = yVal;

		setPanned();
		redraw();
	}

	private void scrollX() {
		ScrollBar hBar = getHorizontalBar();
		int hSelection = hBar.getSelection();
		int offX = (int)(hSelection/currentScale);
		changeImageOffset(offX, currentOffset.y);
	}

	private void scrollY() {
		ScrollBar vBar = getVerticalBar();
		int vSelection = vBar.getSelection();
		int offY = (int)(vSelection/currentScale);
		changeImageOffset(currentOffset.x, offY);
	}

	private void changeScale(int direction, int x, int y) {
		float maxScale = 4f;
		float minScale = 1f/64f;

		boolean zoomIn = direction > 0;

		if (zoomIn && currentScale >= maxScale) return;
		if (!zoomIn && currentScale <= minScale) return;

		int centerX = currentOffset.x + (int)(x / currentScale);
		int centerY = currentOffset.y + (int)(y / currentScale);

		float newScale = currentScale / 2;
		if (zoomIn) newScale = currentScale * 2;

		newScale = Math.max(newScale, minScale);
		newScale = Math.min(newScale, maxScale);

		changeScale(newScale);

		int w = (int)(getClientArea().width/currentScale);
		int h = (int)(getClientArea().height/currentScale);

		changeImageOffset(centerX - (w/2), centerY - (h/2));

		boolean moveCursor = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.AUTO_MOVE_CURSOR);
		if (moveCursor && x > 0 && x < getClientArea().width && y > 0 && y < getClientArea().height) {
			// Move the mouse to the position in which the user scrolled.
			int newX = (int) ((centerX - currentOffset.x) * currentScale);
			int newY = (int) ((centerY - currentOffset.y) * currentScale);
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
			int scaledOffX = (int)(offX/currentScale);
			int scaledOffY = (int)(offY/currentScale);
			if (scaledOffX == 0) {
				if (offX > 0) scaledOffX = 1;
				if (offX < 0) scaledOffX = -1;
			}
			if (scaledOffY == 0) {
				if (offY > 0) scaledOffY = 1;
				if (offY < 0) scaledOffY = -1;
			}
			changeImageOffset(currentOffset.x - scaledOffX, currentOffset.y - scaledOffY);
		}
		previousDragPoint = new Point(x, y);
	}

	private void checkOffsetLimits() {
		// Verify offset is within bounds.
		currentOffset.x = Math.max(currentOffset.x, 0);
		currentOffset.y = Math.max(currentOffset.y, 0);
		int maxOffsetX = currentImageFullSize.x - (int)(getClientArea().width/currentScale);
		int maxOffsetY = currentImageFullSize.y - (int)(getClientArea().height/currentScale);
		if (maxOffsetX > 0) currentOffset.x = Math.min(currentOffset.x, maxOffsetX);
		else currentOffset.x = 0;
		if (maxOffsetY > 0) currentOffset.y = Math.min(currentOffset.y, maxOffsetY);
		else currentOffset.y = 0;
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

	public abstract static class Slice {
		
		public Rectangle bounds;
		
		public Slice(Rectangle bounds) {
			this.bounds = bounds;
		}

		public abstract void drawOnGC(GC gc, Image image, Rectangle target);
	}

}
