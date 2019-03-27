package eu.openanalytics.phaedra.ui.wellimage.canvas;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
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
	private Set<IRenderJob> runningRenderJobs;
	
	private static final Point GRID_CELL_SIZE = new Point(512, 512);
	
	@Override
	public void initialize(ICanvasRenderCallback callback) {
		this.renderCallback = callback;
		this.executor = Executors.newFixedThreadPool(4);
		this.runningRenderJobs = Collections.synchronizedSet(new HashSet<>());
		this.highResRegions = Collections.synchronizedMap(new HashMap<>());
		this.lqScale = 0.1f;
	}
	
	@Override
	public void drawImage(GC gc, Rectangle clientArea, CanvasState canvasState) {
		if (canvasState.getWell() == null 
				|| canvasState.getFullImageSize() == null
				|| canvasState.getFullImageSize().x == 0
				|| canvasState.getFullImageSize().y == 0) return;
		
		try {
			boolean stateChanged = currentCanvasState == null
					|| canvasState.isForceChange() 
					|| canvasState.wellChanged(currentCanvasState)
					|| canvasState.channelsChanged(currentCanvasState)
					|| canvasState.scaleChanged(currentCanvasState);
			
			if (lqFullImage == null || stateChanged) resetState(canvasState);
			
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
				if (highResRegions.containsKey(cellArea)) continue;
				scheduleRenderJob(cellArea, canvasState);
			}

			ImageData imageData = imageRegionGrid.render(targetRenderArea, lqFullImage, highResRegions, canvasState);
			Image img = new Image(null, imageData);
			gc.drawImage(img, 0, 0);
			img.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
		currentCanvasState = canvasState.copy();
		canvasState.setForceChange(false);
	}

	@Override
	public void dispose() {
		// Nothing to dispose
	}

	private void scheduleRenderJob(Rectangle cellArea, CanvasState canvasState) {
		// To prevent duplicate render jobs.
		highResRegions.put(cellArea, null);
		
		HQRenderJob hqRenderJob = new HQRenderJob(canvasState.copy(), cellArea, (job, data) -> {
			highResRegions.put(cellArea, data);
			renderCallback.requestRenderUpdate();
			runningRenderJobs.remove(job);
		});
		
		runningRenderJobs.add(hqRenderJob);
		executor.execute(hqRenderJob);
	}
	
	// Throw away any cached data and start with a fresh image grid.
	private void resetState(CanvasState canvasState) throws IOException {
		lqFullImage = new ImageData(
				(int) (canvasState.getScale() * canvasState.getFullImageSize().x),
				(int) (canvasState.getScale() * canvasState.getFullImageSize().y),
				24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		
		imageRegionGrid = new ImageRegionGrid(canvasState.getFullImageSize(), GRID_CELL_SIZE, canvasState.getScale());
		synchronized (runningRenderJobs) {
			for (IRenderJob job: runningRenderJobs) job.cancel();
		}
		highResRegions.clear();
		
		// Submit an initial job to render the LQ image
		LQRenderJob lqRenderJob = new LQRenderJob(canvasState.copy(), lqScale, (job, data) -> {
			lqFullImage = data;
			renderCallback.requestRenderUpdate();
			runningRenderJobs.remove(job);
		});
		runningRenderJobs.add(lqRenderJob);
		executor.execute(lqRenderJob);
	}
	
	private static interface IRenderJob extends Runnable {
		public void cancel();
	}
	
	private static class LQRenderJob implements IRenderJob {
		
		private CanvasState state;
		private float scale;
		private BiConsumer<IRenderJob, ImageData> callback;
		
		private volatile boolean cancelled;
		
		public LQRenderJob(CanvasState state, float scale, BiConsumer<IRenderJob, ImageData> callback) {
			this.state = state;
			this.scale = scale;
			this.callback = callback;
		}

		@Override
		public void run() {
			if (cancelled) return;
			try {
				ImageData data = ImageRenderService.getInstance().getWellImageData(
						state.getWell(),
						scale,
						state.getChannels());
				if (cancelled) return;
				callback.accept(this, data);
			} catch (IOException e) {
				EclipseLog.error("Failed to render image", e, Activator.PLUGIN_ID);
			}
		}
		
		@Override
		public void cancel() {
			cancelled = true;
		}
	}
	
	private static class HQRenderJob implements IRenderJob {
		
		private CanvasState state;
		private Rectangle region;
		private BiConsumer<IRenderJob, ImageData> callback;
		
		private volatile boolean cancelled;
		
		public HQRenderJob(CanvasState state, Rectangle region, BiConsumer<IRenderJob, ImageData> callback) {
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
				callback.accept(this, data);
			} catch (IOException e) {
				EclipseLog.error("Failed to render image", e, Activator.PLUGIN_ID);
			}
		}
		
		@Override
		public void cancel() {
			cancelled = true;
		}
	}
}
