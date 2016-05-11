package eu.openanalytics.phaedra.base.ui.thumbnailviewer;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.util.highlighter.Highlight;

public class ThumbnailCell {

	private Object data;
	private Thumbnail thumbnail;

	private Rectangle bounds;
	private Highlight highlight;

	public ThumbnailCell(Thumbnail viewer, Object data, Rectangle bounds) {
		this.thumbnail = viewer;
		this.data = data;
		this.bounds = bounds;
		this.highlight = new Highlight();
	}

	public ThumbnailCell(Thumbnail viewer, Object data, int x, int y, int height, int width) {
		this(viewer, data, new Rectangle(x, y, width, height));
	}

	public Object getData() {
		return data;
	}

	public Rectangle getBounds() {
		if (bounds == null) return new Rectangle(0, 0, 0, 0);
		// Add the ScrollOffsets to the bounds before returning them.
		Rectangle newBounds = new Rectangle(bounds.x - thumbnail.gethScrollOffset()
				, bounds.y - thumbnail.getvScrollOffset(), bounds.width, bounds.height);
		return newBounds;
	}

	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
	}

	public void setBounds(int x, int y, int height, int width) {
		setBounds(new Rectangle(x, y, width, height));
	}

	public Highlight getHighlight() {
		return highlight;
	}

	public boolean contains(int x, int y) {
		return getBounds().contains(x, y);
	}

	/**
	 * Paint the cell. If the image is currently not cached, draw a placeholder instead.
	 *
	 * @param gc
	 * @return True if the image was painted (and thus cached). False otherwise.
	 */
	public boolean paint(GC gc) {
		boolean isImageCached = isImageCached();

		Rectangle newBounds = getBounds();
		gc.setClipping(newBounds);
		if (isImageCached) {
			// The image is cached. Retrieve and paint it right now.
			ImageData imgData = thumbnail.getCellRenderer().getImageData(data);
			if (imgData != null) drawImage(gc, newBounds.x, newBounds.y , imgData);
			else gc.fillRectangle(newBounds);
		} else {
			gc.fillRectangle(newBounds);
		}
		highlight.paint(gc, newBounds.x, newBounds.y, newBounds.width, newBounds.height);
		gc.setClipping((Rectangle)null);

		return isImageCached;
	}

	/**
	 * Cache an image. After caching it, draw it.
	 */
	public void cacheAndPaint() {
		ImageData imgData = thumbnail.getCellRenderer().getImageData(data);
		if (imgData == null) return;
		Display.getDefault().asyncExec(() -> {
			if (thumbnail.isDisposed()) return;
			GC gc = new GC(thumbnail);
			Rectangle newBounds = getBounds();
			gc.setClipping(newBounds);
			drawImage(gc, newBounds.x, newBounds.y, imgData);
			gc.dispose();
		});
	}

	private void drawImage(GC gc, int x, int y, ImageData imgData) {
		Image img = null;
		try {
			img = new Image(null, imgData);
			gc.drawImage(img, x, y);
		} finally {
			img.dispose();
		}
	}

	private boolean isImageCached() {
		return thumbnail.getCellRenderer().isImageReady(data);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ThumbnailCell other = (ThumbnailCell) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}

}