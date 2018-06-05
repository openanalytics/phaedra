package eu.openanalytics.phaedra.ui.wellimage.canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.util.misc.ImageUtils;

public class ImageRegionGrid {

	private Point fullImageSize;
	private Point cellSize;
	private List<Rectangle> imageGrid;
	
	public ImageRegionGrid(Point fullImageSize, Point cellSize) {
		this.fullImageSize = fullImageSize;
		this.cellSize = cellSize;
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
		Point clientAreaSize = new Point((int) (targetRenderArea.width * canvasState.getScale()), (int) (targetRenderArea.height * canvasState.getScale()));
		ImageData data = new ImageData(clientAreaSize.x, clientAreaSize.y, lqFullImage.depth, lqFullImage.palette);
		ImageData lqCropScaled = null;
		
		Point startPoint = new Point(targetRenderArea.x, targetRenderArea.y);
		List<Rectangle> cells = getCells(targetRenderArea);
		
		for (Rectangle cell: cells) {
			ImageData cellImageData = highResRegions.get(cell);
			Rectangle cellRenderArea = new Rectangle(startPoint.x, startPoint.y, cell.width - (startPoint.x - cell.x), cell.height - (startPoint.y - cell.y));
			Rectangle cellRenderAreaScaled = new Rectangle(
					(int) (canvasState.getScale() * cellRenderArea.x),
					(int) (canvasState.getScale() * cellRenderArea.y),
					(int) (canvasState.getScale() * cellRenderArea.width),
					(int) (canvasState.getScale() * cellRenderArea.height));

			if (cellImageData == null) {
				if (lqCropScaled == null) {
					float lqScale = ((float) lqFullImage.width) / canvasState.getFullImageSize().x;
					lqCropScaled = ImageUtils.crop(lqFullImage,
							(int) (targetRenderArea.x * lqScale),
							(int) (targetRenderArea.y * lqScale),
							(int) (targetRenderArea.width * lqScale),
							(int) (targetRenderArea.height * lqScale)).scaledTo(clientAreaSize.x, clientAreaSize.y);
				}
				
				for (int y = 0; y < cellRenderAreaScaled.height; y++) {
					int[] line = new int[cellRenderAreaScaled.width];
					
					int offsetX = (int) (canvasState.getScale() * (startPoint.x - targetRenderArea.x));
					int offsetY = y + (int) (canvasState.getScale() * (startPoint.y - targetRenderArea.y));
					int putWidth = Math.min(line.length, lqCropScaled.width - offsetX);
					if (offsetY >= lqCropScaled.height) break;
					lqCropScaled.getPixels(offsetX, offsetY, putWidth, line, 0);
					
					offsetX = (int) (canvasState.getScale() * (startPoint.x - targetRenderArea.x));
					offsetY = y + (int) (canvasState.getScale() * (startPoint.y - targetRenderArea.y));
					
					if (offsetY >= data.height) break;
					putWidth = Math.min(line.length, data.width - offsetX);
					data.setPixels(offsetX, offsetY, putWidth, line, 0);
				}
			} else {
				for (int y = 0; y < cellRenderAreaScaled.height; y++) {
					int[] line = new int[cellRenderAreaScaled.width];
					
					int offsetX = (int) (canvasState.getScale() * (startPoint.x - cell.x));
					int offsetY = y + (int) (canvasState.getScale() * (startPoint.y - cell.y));
					cellImageData.getPixels(offsetX, offsetY, line.length, line, 0);
					
					offsetX = (int) (canvasState.getScale() * (startPoint.x - targetRenderArea.x));
					offsetY = y + (int) (canvasState.getScale() * (startPoint.y - targetRenderArea.y));
					
					if (offsetY >= data.height) break;
					int putWidth = Math.min(line.length, data.width - offsetX);
					data.setPixels(offsetX, offsetY, putWidth, line, 0);
				}
			}
			
			int cellIndex = cells.indexOf(cell);
			if (cellIndex + 1 < cells.size()) {
				Rectangle nextCell = cells.get(cellIndex + 1);
				if (nextCell.y == cell.y) {
					// Next cell is to the right of this one.
					startPoint.x += cellRenderArea.width;
				} else {
					// Next cell is on the next row.
					startPoint.x = targetRenderArea.x;
					startPoint.y += cellRenderArea.height;
				}
			}
		}
		
		return data;
	}
	
	private void calculateGrid() {
		imageGrid = new ArrayList<>();
		int x = 0;
		int y = 0;
		while (y < fullImageSize.y) {
			while (x < fullImageSize.x) {
				Rectangle cell = new Rectangle(x, y, cellSize.x, cellSize.y);
				imageGrid.add(cell);
				x += cellSize.x;
			}
			x = 0;
			y += cellSize.y;
		}
	}
}
