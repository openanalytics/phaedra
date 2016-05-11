package eu.openanalytics.phaedra.base.ui.colormethod;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class ColorMethodLegend extends Canvas {

	private IColorMethod colorMethod;
	private Image currentImage;
	private int orientation;

	private double[] highlightValues;

	private boolean showLabels;

	private final static int MIN_SIZE = 100;

	public ColorMethodLegend(Composite parent, int style, boolean showLabels) {
		super(parent, SWT.DOUBLE_BUFFERED);

		this.showLabels = showLabels;

		if ((style & SWT.HORIZONTAL) != 0) orientation = SWT.HORIZONTAL;
		else orientation = SWT.VERTICAL;

		addListener(SWT.Paint, e -> renderLegend(e.gc));
		addListener(SWT.Resize, e -> updateSize());
		addListener(SWT.Dispose, e -> {
			if (currentImage != null && !currentImage.isDisposed()) {
				currentImage.dispose();
			}
		});
	}

	public void setHighlightValues(double[] highlightValues) {
		this.highlightValues = highlightValues;
	}

	public void setColorMethod(IColorMethod colorMethod) {
		this.colorMethod = colorMethod;
		createLegendImage();
		redraw();
	}

	private void updateSize() {
		Point size = getSize();
		if (orientation == SWT.VERTICAL && size.x < MIN_SIZE) {
			size.x = MIN_SIZE;
			setSize(size);
		} else if (orientation == SWT.HORIZONTAL && size.y < MIN_SIZE) {
			size.y = MIN_SIZE;
			setSize(size);
		} else {
			createLegendImage();
		}
		redraw();
	}

	private void createLegendImage() {
		if (currentImage != null && !currentImage.isDisposed()) {
			currentImage.dispose();
			currentImage = null;
		}
		if (colorMethod != null) {
			Point size = getSize();
			if (size.x > 0 && size.y > 0) {
				currentImage = colorMethod.getLegend(
						size.x, size.y, orientation, showLabels, highlightValues);
			}
		}
	}

	private void renderLegend(GC gc) {
		if (currentImage == null) return;
		gc.drawImage(currentImage, 0, 0);
	}
}
