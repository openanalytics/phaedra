package eu.openanalytics.phaedra.base.ui.gridviewer.concurrent;

import org.eclipse.swt.graphics.ImageData;

public class ConcurrentTaskResult {
	
	private final int row, column;
	private ImageData data;

	public ConcurrentTaskResult(int row, int column, ImageData data) {
		this.row = row;
		this.column = column;
		this.data = data;
	}

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}

	public ImageData getImageData() {
		return data;
	}
}
