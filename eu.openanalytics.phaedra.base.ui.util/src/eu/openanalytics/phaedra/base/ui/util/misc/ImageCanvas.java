package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class ImageCanvas extends Canvas {

	private int alignment;

	private ImageData imageData;

	private int hScrollOffset;
	private int vScrollOffset;
	private int maxX;
	private int maxY;
	private int clientAreaWidth;
	private int clientAreaHeight;

	private boolean isDragging;
	private Point previousDragPoint;

	public ImageCanvas(Composite parent, int style) {
		this(parent, style, SWT.LEFT);
	}

	public ImageCanvas(Composite parent, int style, int alignment) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.H_SCROLL | SWT.V_SCROLL);

		this.alignment = alignment;

		addPaintListener(e -> {
			GC gc = e.gc;
			gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			gc.fillRectangle(0, 0, clientAreaWidth, clientAreaHeight);
			paint(gc);
		});
		addListener(SWT.Resize, e -> {
			Rectangle clientArea = getClientArea();
			clientAreaWidth = clientArea.width;
			clientAreaHeight = clientArea.height;
			updateHScroll();
			updateVScroll();
		});

		addListener(SWT.MouseDown, e -> {
			Cursor dragCursor = Display.getDefault().getSystemCursor(SWT.CURSOR_SIZEALL);
			isDragging = true;
			previousDragPoint = new Point(e.x, e.y);
			setCursor(dragCursor);
		});
		addListener(SWT.MouseUp, e -> {
			isDragging = false;
			previousDragPoint = null;
			setCursor(null);
		});
		addMouseMoveListener(e -> {
			if (isDragging) {
				if (previousDragPoint != null) {
					int offX = e.x - previousDragPoint.x;
					int offY = e.y - previousDragPoint.y;
					hScrollOffset = Math.max(hScrollOffset + offX, 0);
					vScrollOffset = Math.max(vScrollOffset + offY, 0);
					updateHScroll();
					updateVScroll();
					redraw();
				}
				previousDragPoint = new Point(e.x, e.y);
			}
		});

		getHorizontalBar().addListener(SWT.Selection, e -> scrollH());
		getVerticalBar().addListener(SWT.Selection, e -> scrollV());
	}

	public void setImageData(ImageData imageData) {
		if (imageData != null) {
			this.imageData = imageData;
			this.maxX = imageData.width;
			this.maxY = imageData.height;
		} else {
			this.imageData = null;
			this.maxX = 0;
			this.maxY = 0;
		}
		updateHScroll();
		updateVScroll();
		redraw();
	}

	private void paint(GC gc) {
		if (imageData != null) {
			Image img = new Image(null, imageData);
			try {
				if ((alignment & SWT.LEFT) != 0) {
					gc.drawImage(img, -hScrollOffset, -vScrollOffset);
				}
				if ((alignment & SWT.CENTER) != 0) {
					int x = 0, y = 0;
					if (imageData.width > clientAreaWidth) x = -hScrollOffset;
					else x = (clientAreaWidth / 2) - (imageData.width / 2);
					if (imageData.height > clientAreaHeight) y = -vScrollOffset;
					else y = (clientAreaHeight / 2) - (imageData.height / 2);
					gc.drawImage(img, x, y);
				}
				if ((alignment & SWT.RIGHT) != 0) {
					int x = 0, y = 0;
					if (imageData.width > clientAreaWidth) x = -hScrollOffset;
					else x = (clientAreaWidth - imageData.width);
					if (imageData.height > clientAreaHeight) y = -vScrollOffset;
					else y = (clientAreaHeight - imageData.height);
					gc.drawImage(img, x, y);
				}
			} finally {
				img.dispose();
			}
		}
	}

	private void updateHScroll() {
		if (maxX > clientAreaWidth) {
			getHorizontalBar().setEnabled(true);
			getHorizontalBar().setVisible(true);
			hScrollOffset = Math.min(hScrollOffset, maxX - clientAreaWidth);
			getHorizontalBar().setValues(hScrollOffset, 0, maxX, clientAreaWidth, 50, clientAreaWidth);
		} else {
			hScrollOffset = 0;
			getHorizontalBar().setEnabled(false);
			getHorizontalBar().setVisible(false);
		}
	}

	private void updateVScroll() {
		if (maxY > clientAreaHeight) {
			getVerticalBar().setEnabled(true);
			getVerticalBar().setVisible(true);
			vScrollOffset = Math.min(vScrollOffset, maxY - clientAreaHeight);
			getVerticalBar().setValues(vScrollOffset, 0, maxY, clientAreaHeight, 50, clientAreaHeight);
		} else {
			vScrollOffset = 0;
			getVerticalBar().setEnabled(false);
			getVerticalBar().setVisible(false);
		}
	}

	private void scrollH() {
		hScrollOffset = getHorizontalBar().getSelection();
		redraw();
	}

	private void scrollV() {
		vScrollOffset = getVerticalBar().getSelection();
		redraw();
	}

}