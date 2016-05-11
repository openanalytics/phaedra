package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class CustomToggleButton extends Canvas {

	private boolean toggle;
	private Image image;
	private String text;
	
	public CustomToggleButton(Composite parent, int style) {
		super(parent, style);
		
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				doPaint(e.gc);
			}
		});
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				doDispose();
			}
		});
		
		toggle = true;
	}
	
	public boolean toggle() {
		toggle = !toggle;
		redraw();
		return toggle;
	}
	
	public void setImage(Image img) {
		if (image != null && !image.isDisposed()) {
			image.dispose();
		}
		image = img;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	private void doPaint(GC gc) {
		if (image != null && !image.isDisposed()) {
			gc.drawImage(image, 0, 0);
		}
		gc.drawRectangle(0, 0, getSize().x, getSize().y);
		
		if (text != null) {
			Point extent = gc.textExtent(text);
			int x = (getSize().x / 2) - (extent.x / 2);
			int y = (getSize().y / 2) - (extent.y / 2);
			gc.drawText(text, x, y, true);
		}
		if (!toggle) paintCross(gc);
	}
	
	private void doDispose() {
		if (image != null && !image.isDisposed()) image.dispose();
	}
	
	private void paintCross(GC gc) {
		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		int lineWidth = gc.getLineWidth();
		gc.setLineWidth(2);
		int w = getSize().x;
		int h = getSize().y;
		
		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		gc.fillOval(2, 2, w-4, h-5);
		
		gc.drawOval(2, 2, w-5, h-6);
		gc.drawLine(4, 4, w-4, h-5);
		gc.setLineWidth(lineWidth);
	}
}
