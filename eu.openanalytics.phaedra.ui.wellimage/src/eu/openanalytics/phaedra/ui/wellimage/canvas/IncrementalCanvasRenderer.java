package eu.openanalytics.phaedra.ui.wellimage.canvas;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.ui.wellimage.Activator;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageRegionGrid;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class IncrementalCanvasRenderer implements ICanvasRenderer {

	private CanvasState currentCanvasState;
	private ICanvasRenderCallback renderCallback;
	private ExecutorService executor;
	
	private float lqScale;
	private ImageData lqFullImage;
	private ImageRegionGrid imageRegionGrid;
	private Map<Rectangle, ImageData> highResRegions;
	
	private static final Point GRID_CELL_SIZE = new Point(512, 512);
	
	@Override
	public void initialize(ICanvasRenderCallback callback) {
		this.renderCallback = callback;
		this.executor = Executors.newSingleThreadExecutor();
		this.highResRegions = new HashMap<>();
		this.lqScale = 0.1f;
	}
	
	@Override
	public void drawImage(GC gc, Rectangle clientArea, CanvasState canvasState) {
		if (canvasState.getWell() == null) return;
		
		try {
			if (lqFullImage == null || canvasState.wellChanged(currentCanvasState) || canvasState.channelsChanged(currentCanvasState)) {
					lqFullImage = ImageRenderService.getInstance().getWellImageData(canvasState.getWell(), lqScale, canvasState.getChannels());
					imageRegionGrid = new ImageRegionGrid(canvasState.getFullImageSize(), GRID_CELL_SIZE);
					highResRegions.clear();
			}
			
			ImageData imageData = null;
			
			//TODO Check highResRegions and use any regions that apply
			Rectangle hqArea = new Rectangle(0, 0, canvasState.getFullImageSize().x, canvasState.getFullImageSize().y);
			
			
			if (highResRegions.isEmpty()) {
				RenderJob hqRenderJob = new RenderJob(canvasState.copy(), hqArea, data -> {
					highResRegions.put(hqArea, data);
					renderCallback.requestRenderUpdate();
				});
				executor.execute(hqRenderJob);
				imageData = getScaledImage(lqFullImage, lqScale, canvasState, clientArea);
			} else {
				imageData = getScaledImage(highResRegions.values().iterator().next(), 1.0f, canvasState, clientArea);
			}
			
			Image img = new Image(null, imageData);
			gc.drawImage(img, 0, 0);
			img.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
		currentCanvasState = canvasState.copy();
	}

	@Override
	public void dispose() {
		// Nothing to dispose
	}

	private ImageData getScaledImage(ImageData fullImage, float fullImageScale, CanvasState canvasState, Rectangle clientArea) {
		float scaleFactor = canvasState.getScale() / fullImageScale;
		
		Point lqFullImageSize = new Point(
				(int) Math.ceil(canvasState.getFullImageSize().x * fullImageScale),
				(int) Math.ceil(canvasState.getFullImageSize().y * fullImageScale));
		
		Rectangle lqRenderArea = new Rectangle(
				(int) Math.ceil(canvasState.getOffset().x * fullImageScale),
				(int) Math.ceil(canvasState.getOffset().y * fullImageScale),
				Math.min(lqFullImageSize.x, (int) Math.ceil(clientArea.width / scaleFactor)),
				Math.min(lqFullImageSize.y, (int) Math.ceil(clientArea.height / scaleFactor)));
		
		ImageData lqData = ImageUtils.crop(fullImage, lqRenderArea.x, lqRenderArea.y, lqRenderArea.width, lqRenderArea.height);

		Point scaledSize = new Point(
				(int) (lqRenderArea.width * scaleFactor),
				(int) (lqRenderArea.height * scaleFactor));
		ImageData scaledData = lqData.scaledTo(scaledSize.x, scaledSize.y);
		return scaledData;
	}
	
	private static class RenderJob implements Runnable {
		
		private CanvasState state;
		private Rectangle region;
		private Consumer<ImageData> callback;
		
		private volatile boolean cancelled;
		
		public RenderJob(CanvasState state, Rectangle region, Consumer<ImageData> callback) {
			this.state = state;
			this.region = region;
			this.callback = callback;
		}

		@Override
		public void run() {
			if (cancelled) return;
			try {
				ImageData data = ImageRenderService.getInstance().getWellImageData(
						state.getWell(),
						1.0f,
						region,
						state.getChannels());
				if (cancelled) return;
				callback.accept(data);
			} catch (IOException e) {
				EclipseLog.error("Failed to render image", e, Activator.PLUGIN_ID);
			}
		}
		
		public void cancel() {
			cancelled = true;
		}
	}
}
