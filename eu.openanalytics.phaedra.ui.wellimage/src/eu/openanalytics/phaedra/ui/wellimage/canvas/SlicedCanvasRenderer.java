package eu.openanalytics.phaedra.ui.wellimage.canvas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.ui.wellimage.Activator;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

/**
 * Renders an image on a canvas using a set of rectangles (slices).
 * Scrolling the canvas results in small slices being added/removed, which minimizes the number of 'new' pixels needing to be rendered.
 * The downside is that these slices are inefficient in the image cache, as they are rarely reused in later render calls.
 */
public class SlicedCanvasRenderer implements ICanvasRenderer {

	private Image currentImage;
	private Rectangle currentRenderArea;
	private CanvasState currentCanvasState;
	
	@Override
	public void drawImage(GC gc, Rectangle clientArea, CanvasState canvasState) {
		if (canvasState.getWell() == null) return;

		//TODO If the clientArea changes but the canvas state doesn't, we still should render new pixels.
		// If there are no changes in the state, just redraw the current image.
		if (!canvasState.anyChanged(currentCanvasState) && currentImage != null && !currentImage.isDisposed()) {
			gc.drawImage(currentImage, 0, 0);
			return;
		}
		
		// If the image contents change, the current image must be disposed.
		boolean shouldDispose = canvasState.isForceChange() 
				|| canvasState.wellChanged(currentCanvasState)
				|| canvasState.scaleChanged(currentCanvasState)
				|| canvasState.channelsChanged(currentCanvasState);
		if (shouldDispose) disposeCurrentImage();
		
		try {
			// Find out which area of the image to render.
			Rectangle targetRenderArea = new Rectangle(
					canvasState.getOffset().x,
					canvasState.getOffset().y,
					(int) Math.ceil(clientArea.width / canvasState.getScale()),
					(int) Math.ceil(clientArea.height / canvasState.getScale()));

			// If the client area is bigger than the image area, reduce image area a bit.
			if (targetRenderArea.width > canvasState.getFullImageSize().x) targetRenderArea.width = canvasState.getFullImageSize().x;
			if (targetRenderArea.height > canvasState.getFullImageSize().y) targetRenderArea.height = canvasState.getFullImageSize().y;

			Image newImage = new Image(null, clientArea.width, clientArea.height);

			// Reuse the part of the current image that is still on-screen (if any)
			if (currentImage != null) {
				int deltaX = (int) (canvasState.getScale() * (currentRenderArea.x - canvasState.getOffset().x));
				int deltaY = (int) (canvasState.getScale() * (currentRenderArea.y - canvasState.getOffset().y));

				GC imageGC = new GC(newImage);
				imageGC.drawImage(currentImage, deltaX, deltaY);
				imageGC.dispose();

				currentImage.dispose();
			}

			// Calculate missing parts between current and target area
			for (Slice slice : subtract(currentRenderArea, targetRenderArea, canvasState)) {
				ImageData data = ImageRenderService.getInstance().getWellImageData(canvasState.getWell(), canvasState.getScale(), slice.bounds, canvasState.getChannels());
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
			currentCanvasState = canvasState.copy();
			
			canvasState.setForceChange(false);
		} catch (Exception e) {
			EclipseLog.error("Failed to render image", e, Activator.PLUGIN_ID);
		}
	}

	@Override
	public void dispose() {
		disposeCurrentImage();
	}
	
	private void disposeCurrentImage() {
		if (currentImage != null && !currentImage.isDisposed()) currentImage.dispose();
		currentImage = null;
		currentRenderArea = null;
	}
	
	private Collection<Slice> subtract(Rectangle original, Rectangle target, CanvasState canvasState) {
		if (original == null) original = new Rectangle(0, 0, 0, 0);
		
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
						int y = target.height - (int)(bounds.height * canvasState.getScale());
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
						int x = target.width - (int)(bounds.width * canvasState.getScale());
						gc.drawImage(image, x, target.y);
					}
				});
			}
		}

		return slices;
	}

	private abstract static class Slice {
		
		public Rectangle bounds;
		
		public Slice(Rectangle bounds) {
			this.bounds = bounds;
		}

		public abstract void drawOnGC(GC gc, Image image, Rectangle target);
	}
	
}
