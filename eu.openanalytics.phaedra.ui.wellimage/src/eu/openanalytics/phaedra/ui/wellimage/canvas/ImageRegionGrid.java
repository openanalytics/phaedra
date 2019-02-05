package eu.openanalytics.phaedra.ui.wellimage.canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;

public class ImageRegionGrid {

	private Point fullImageSize;
	private Point cellSize;
	private float scale;
	private List<Rectangle> imageGrid;
	
	public ImageRegionGrid(Point fullImageSize, Point cellSize, float scale) {
		this.fullImageSize = fullImageSize;
		this.cellSize = cellSize;
		this.scale = scale;
		calculateGrid();
	}

	public List<Rectangle> getCells(Rectangle imageRegion) {
		List<Rectangle> matches = new ArrayList<>();
		for (Rectangle cell: imageGrid) {
			if (cell.intersects(imageRegion)) matches.add(cell);
 		}
		return matches;
	}
	
	public ImageData render(Rectangle targetRenderArea, ImageData lqFullImage, Map<Rectangle, ImageData> highResRegions, CanvasState canvasState) {
		float renderScale = canvasState.getScale();
		Point renderAreaSize = SWTUtils.scale(targetRenderArea.width, targetRenderArea.height, renderScale);
		
		ImageData renderData = new ImageData(renderAreaSize.x, renderAreaSize.y, lqFullImage.depth, lqFullImage.palette);
		ImageData lqCropScaled = null;
		
		List<Rectangle> cells = getCells(targetRenderArea);
		Point cellStartPoint = new Point(targetRenderArea.x, targetRenderArea.y);
		
		for (Rectangle cell: cells) {
			ImageData cellImageData = highResRegions.get(cell);
			Point cellRenderSize = new Point(cell.width - (cellStartPoint.x - cell.x), cell.height - (cellStartPoint.y - cell.y));
			Point cellRenderSizeScaled = SWTUtils.scale(cellRenderSize, renderScale);
			
			if (cellImageData == null) {
				if (lqCropScaled == null) {
					// First time accessing the lq data: get the cropped, scaled pixels
					float lqFullImageScale = ((float) lqFullImage.width) / canvasState.getFullImageSize().x;
					Rectangle lqRegion = SWTUtils.scale(targetRenderArea, lqFullImageScale);
					lqCropScaled = ImageUtils.crop(lqFullImage, lqRegion).scaledTo(renderAreaSize.x, renderAreaSize.y);
				}
				Point readOffset = SWTUtils.scale(cellStartPoint.x - targetRenderArea.x, cellStartPoint.y - targetRenderArea.y, renderScale);
				copyPixels(lqCropScaled, renderData, readOffset, readOffset, cellRenderSizeScaled);
			} else {
				Point readOffset = SWTUtils.scale(cellStartPoint.x - cell.x, cellStartPoint.y - cell.y, renderScale);
				Point writeOffset = SWTUtils.scale(cellStartPoint.x - targetRenderArea.x, cellStartPoint.y - targetRenderArea.y, renderScale);
				copyPixels(cellImageData, renderData, readOffset, writeOffset, cellRenderSizeScaled);
			}
			
			// Move the cellStartPoint into the next cell.
			int cellIndex = cells.indexOf(cell);
			if (cellIndex + 1 < cells.size()) {
				Rectangle nextCell = cells.get(cellIndex + 1);
				if (nextCell.y == cell.y) {
					// Next cell is to the right of this one.
					cellStartPoint.x += cellRenderSize.x;
				} else {
					// Next cell is on the next row.
					cellStartPoint.x = targetRenderArea.x;
					cellStartPoint.y += cellRenderSize.y;
				}
			}
		}
		
		return renderData;
	}
	
	private void copyPixels(ImageData from, ImageData to, Point readOffset, Point writeOffset, Point maxSize) {
		// Don't render beyond the render area (horizontally)
		int readWidth = from.width - readOffset.x;
		int writeWidth = to.width - writeOffset.x;
		int lineWidth = Math.min(Math.min(readWidth, writeWidth), maxSize.x);
		
		int[] line = new int[lineWidth];
		for (int y = 0; y < maxSize.y; y++) {
			// Don't render beyond the render area (vertically)
			if (y + writeOffset.y >= to.height) break;
			
			from.getPixels(readOffset.x, y + readOffset.y, lineWidth, line, 0);
			to.setPixels(writeOffset.x, y + writeOffset.y, lineWidth, line, 0);
		}
	}
	
	private void calculateGrid() {
		imageGrid = new ArrayList<>();
		int x = 0;
		int y = 0;
		Point scaledCellSize = SWTUtils.scale(cellSize, 1.0f/scale);
		
		while (y < fullImageSize.y) {
			while (x < fullImageSize.x) {
				int width = Math.min(scaledCellSize.x, fullImageSize.x - x);
				int height = Math.min(scaledCellSize.y, fullImageSize.y - y);
				Rectangle cell = new Rectangle(x, y, width, height);
				imageGrid.add(cell);
				x += scaledCellSize.x;
			}
			x = 0;
			y += scaledCellSize.y;
		}
		System.out.println("Generating image grid: " + fullImageSize + ", cell size: " + scaledCellSize + " = " + imageGrid.size());
	}
}
