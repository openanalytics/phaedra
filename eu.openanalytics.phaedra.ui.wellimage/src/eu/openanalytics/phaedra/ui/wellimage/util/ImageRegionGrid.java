package eu.openanalytics.phaedra.ui.wellimage.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

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
