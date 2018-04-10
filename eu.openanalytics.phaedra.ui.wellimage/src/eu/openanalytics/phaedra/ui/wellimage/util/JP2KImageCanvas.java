package eu.openanalytics.phaedra.ui.wellimage.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

	private Well currentWell;
	private Image cachedImg;

	private Point imageOffset;
	private Point totalImageSize;

	private float currentScale;

	private boolean draggingEnabled;
	private boolean dragging;
	private Cursor dragCursor;
	private Point previousDragPoint;

	private ListenerManager listenerManager;
	private IUIEventListener imageSettingsListener;

	private JP2KOverlay[] overlays;
	private int oldWidth;
	private int oldHeight;
	private boolean hasPanned;
	private Point oldOffset;
	private boolean[] oldEnabledChannels;

	private int lastYScrollBarInteration;
	private boolean isYScrollBarCorrection;
	private int lastScrollBarPosition;

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
		imageOffset = new Point(0,0);
		totalImageSize = new Point(0,0);
		currentScale = 1.0f;
		dragCursor = Display.getDefault().getSystemCursor(SWT.CURSOR_SIZEALL);
		listenerManager.onOffsetChange(imageOffset.x, imageOffset.y);

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
				changeImageOffset((int) (imageOffset.x - (totalImageSize.x * ARROW_KEY_MOVEMENT)), imageOffset.y);
				break;
			case SWT.ARROW_RIGHT:
				changeImageOffset((int) (imageOffset.x + (totalImageSize.x * ARROW_KEY_MOVEMENT)), imageOffset.y);
				break;
			case SWT.ARROW_UP:
				changeImageOffset(imageOffset.x, (int) (imageOffset.y - (totalImageSize.y * ARROW_KEY_MOVEMENT)));
				break;
			case SWT.ARROW_DOWN:
				changeImageOffset(imageOffset.x, (int) (imageOffset.y + (totalImageSize.y * ARROW_KEY_MOVEMENT)));
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
			totalImageSize = ImageRenderService.getInstance().getWellImageSize(well, 1.0f);
			// If the protocol class changed and the canvas is not linked to another canvas, apply the protocol class' default scale.
			if (!restoredView && !samePClass && !LinkedImageManager.getInstance().isLinked(this)) {
				ImageSettings currentSettings = ImageSettingsService.getInstance().getCurrentSettings(well);
				changeScale(1f/currentSettings.getZoomRatio());
			}
			// The new image may have a size different from the previous. Verify the offsets.
			checkOffsetLimits();
			listenerManager.onOffsetChange(imageOffset.x, imageOffset.y);

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
		return imageOffset;
	}

	public Well getCurrentWell() {
		return currentWell;
	}

	public Point getCurrentImageSize() {
		return totalImageSize;
	}

	// All in one solution for Reporting
	public void changeScaleAndImageOffset(float newScale, int x, int y) {
		currentScale = newScale;
		imageOffset.x = x;
		imageOffset.y = y;
		checkOffsetLimits();
		listenerManager.onScaleChange(currentScale);
		listenerManager.onOffsetChange(imageOffset.x, imageOffset.y);
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
		int oldX = imageOffset.x;
		int oldY = imageOffset.y;
		if (oldX == x && oldY == y) return;
		imageOffset.x = x;
		imageOffset.y = y;
		checkOffsetLimits();
		// Prevents endless loop if new offset is outside legal range.
		if (imageOffset.x == oldX && imageOffset.y == oldY) return;
		listenerManager.onOffsetChange(imageOffset.x, imageOffset.y);
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
					ImageData data = ImageRenderService.getInstance().getWellImageData(currentWell, currentScale, oldEnabledChannels);
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
		oldWidth = 0;
		oldHeight = 0;
		oldOffset = new Point(0,0);
		hasPanned = false;
		oldEnabledChannels = null;
		if (cachedImg != null) cachedImg.dispose();
		cachedImg = null;
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

		if (!hasPanned && cachedImg != null && !cachedImg.isDisposed()) {
			gc.drawImage(cachedImg, 0, 0);
			return;
		}

		try {
			Rectangle cachedRect = new Rectangle(oldOffset.x, oldOffset.y, oldWidth, oldHeight);

			// Find out which part of the image to render.
			Rectangle clientArea = getClientArea();
			int w = (int) Math.ceil(clientArea.width/currentScale);
			int h = (int) Math.ceil(clientArea.height/currentScale);
			if (w > totalImageSize.x) w = totalImageSize.x;
			if (h > totalImageSize.y) h = totalImageSize.y;

			int needed = (int)(currentScale*(totalImageSize.x - imageOffset.x));
			int available = getClientArea().width;
			if (needed < available && imageOffset.x > 0) {
				// We need to reduce offset
				imageOffset.x = Math.max(0, totalImageSize.x - (int)(available/currentScale));
				for (JP2KOverlay o: overlays) o.setOffset(imageOffset);
			}

			Rectangle targetRect = new Rectangle(imageOffset.x, imageOffset.y, w, h);
			int deltaX = oldOffset.x - imageOffset.x;
			int deltaY = oldOffset.y - imageOffset.y;
			deltaX *= currentScale;
			deltaY *= currentScale;

			// Pan cachedImage
			if (cachedImg != null){
				Image buffer = cachedImg;
				cachedImg = new Image(getDisplay(), clientArea.width, clientArea.height);

				GC imageGC = new GC(cachedImg);
				imageGC.drawImage(buffer, deltaX, deltaY);
				imageGC.dispose();

				buffer.dispose();
			} else {
				cachedImg = new Image(getDisplay(), clientArea.width, clientArea.height);
			}

			// Determine which channels to render.
			ImageSettings currentSettings = ImageSettingsService.getInstance().getCurrentSettings(currentWell);
			List<ImageChannel> channels = currentSettings.getImageChannels();
			boolean[] enabledChannels = new boolean[channels.size()];
			for (int i=0; i<channels.size(); i++) {
				enabledChannels[i] = isChannelEnabled(channels.get(i).getSequence());
			}

			for (Slice slice : subtract(cachedRect, targetRect)) {
				// Draw leftover on cachedImage
				ImageData data = ImageRenderService.getInstance().getWellImageData(currentWell, currentScale, slice.bounds, enabledChannels);
				Image leftoverImage = new Image(null, data);

				GC imageGC = new GC(cachedImg);
				slice.drawOnGC(imageGC, leftoverImage, clientArea);
				imageGC.dispose();
				leftoverImage.dispose();
			}

			if (cachedImg != null) gc.drawImage(cachedImg, 0, 0);

			// Update cache values
			oldOffset = new Point(imageOffset.x, imageOffset.y);
			oldWidth = w;
			oldHeight = h;
			if (oldEnabledChannels != null && !Arrays.equals(oldEnabledChannels, enabledChannels)) {
				clearCacheValues();
				updateScrollbars();
				forceRedraw();
			} else {
				hasPanned = false;
			}

			oldEnabledChannels = enabledChannels;

		} catch (Exception e) {
			// Didn't work.
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
		int scaledImageX = (int)(totalImageSize.x*currentScale);
		int scaledImageY = (int)(totalImageSize.y*currentScale);

		ScrollBar hBar = getHorizontalBar();
		ScrollBar vBar = getVerticalBar();
		Rectangle client = getClientArea();
		hBar.setMaximum(scaledImageX);
		vBar.setMaximum(scaledImageY);
		hBar.setThumb(Math.min(scaledImageX, client.width));
		vBar.setThumb(Math.min(scaledImageY, client.height));
		hBar.setPageIncrement(hBar.getThumb());
		vBar.setPageIncrement(vBar.getThumb());
		int xVal = (int)(scaledImageX*((double)imageOffset.x/totalImageSize.x));
		int yVal = (int)(scaledImageY*((double)imageOffset.y/totalImageSize.y));
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
		changeImageOffset(offX, imageOffset.y);
	}

	private void scrollY() {
		ScrollBar vBar = getVerticalBar();
		int vSelection = vBar.getSelection();
		int offY = (int)(vSelection/currentScale);
		changeImageOffset(imageOffset.x, offY);
	}

	private void changeScale(int direction, int x, int y) {
		float maxScale = 4f;
		float minScale = 1f/64f;

		boolean zoomIn = direction > 0;

		if (zoomIn && currentScale >= maxScale) return;
		if (!zoomIn && currentScale <= minScale) return;

		int centerX = imageOffset.x + (int)(x / currentScale);
		int centerY = imageOffset.y + (int)(y / currentScale);

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
			int newX = (int) ((centerX - imageOffset.x) * currentScale);
			int newY = (int) ((centerY - imageOffset.y) * currentScale);
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
			changeImageOffset(imageOffset.x - scaledOffX, imageOffset.y - scaledOffY);
		}
		previousDragPoint = new Point(x, y);
	}

	private void checkOffsetLimits() {
		// Verify offset is within bounds.
		imageOffset.x = Math.max(imageOffset.x, 0);
		imageOffset.y = Math.max(imageOffset.y, 0);
		int maxOffsetX = totalImageSize.x - (int)(getClientArea().width/currentScale);
		int maxOffsetY = totalImageSize.y - (int)(getClientArea().height/currentScale);
		if (maxOffsetX > 0) imageOffset.x = Math.min(imageOffset.x, maxOffsetX);
		else imageOffset.x = 0;
		if (maxOffsetY > 0) imageOffset.y = Math.min(imageOffset.y, maxOffsetY);
		else imageOffset.y = 0;
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
