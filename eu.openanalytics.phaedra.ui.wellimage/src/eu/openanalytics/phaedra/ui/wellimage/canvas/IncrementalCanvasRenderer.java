package eu.openanalytics.phaedra.ui.wellimage.canvas;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import eu.openanalytics.phaedra.ui.wellimage.Activator;
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
		this.executor = Executors.newFixedThreadPool(4);
		this.highResRegions = Collections.synchronizedMap(new HashMap<>());
		this.lqScale = 0.1f;
	}
	
	@Override
	public void drawImage(GC gc, Rectangle clientArea, CanvasState canvasState) {
		if (canvasState.getWell() == null) return;
		
		try {
			boolean stateChanged = currentCanvasState == null
					|| canvasState.isForceChange() 
					|| canvasState.wellChanged(currentCanvasState)
					|| canvasState.channelsChanged(currentCanvasState)
					|| canvasState.scaleChanged(currentCanvasState);
			
			if (lqFullImage == null || stateChanged) {
					lqFullImage = ImageRenderService.getInstance().getWellImageData(canvasState.getWell(), lqScale, canvasState.getChannels());
					imageRegionGrid = new ImageRegionGrid(canvasState.getFullImageSize(), GRID_CELL_SIZE);
					highResRegions.clear();
			}
			
			// Find out which area of the image to render.
			Rectangle targetRenderArea = new Rectangle(
					canvasState.getOffset().x,
					canvasState.getOffset().y,
					(int) Math.ceil(clientArea.width / canvasState.getScale()),
					(int) Math.ceil(clientArea.height / canvasState.getScale()));
			// Ensure the render area does not exceed the image dimensions.
			targetRenderArea.width = Math.min(targetRenderArea.width, canvasState.getFullImageSize().x - targetRenderArea.x);
			targetRenderArea.height = Math.min(targetRenderArea.height, canvasState.getFullImageSize().y - targetRenderArea.y);
			
			List<Rectangle> cellAreas = imageRegionGrid.getCells(targetRenderArea);
			
			// Submit render jobs for any missing cells.
			for (Rectangle cellArea: cellAreas) {
				if (!highResRegions.containsKey(cellArea)) {
					highResRegions.put(cellArea, null); // To prevent duplicate render jobs.
					RenderJob hqRenderJob = new RenderJob(canvasState.copy(), cellArea, data -> {
						highResRegions.put(cellArea, data);
						renderCallback.requestRenderUpdate();
					});
					executor.execute(hqRenderJob);
				}
			}

			ImageData imageData = imageRegionGrid.render(targetRenderArea, lqFullImage, highResRegions, canvasState);
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
						state.getScale(),
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
