package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.google.common.base.Objects;

import eu.openanalytics.phaedra.base.util.misc.FormulaDescriptor;


public class FormulaDisplay extends Canvas implements IFormulaRenderer {
	
	
	private FormulaDescriptor descriptor;
	
	private Color backgroundColor;
	
	private Image image;
	
	
	public FormulaDisplay(Composite parent) {
		super(parent, SWT.DOUBLE_BUFFERED);
		
		addPaintListener(this::paintFormula);
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				disposeImage();
			}
		});
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				disposeImage();
			}
		});
		
		backgroundColor = getDisplay().getSystemColor(SWT.COLOR_WHITE);
	}
	
	
	public void setFormula(FormulaDescriptor descriptor) {
		if (Objects.equal(this.descriptor, descriptor)) {
			return;
		}
		this.descriptor = descriptor;
		
		disposeImage();
		
		if (!isDisposed()) {
			redraw();
		}
	}
	
	
	private void paintFormula(PaintEvent event) {
		updateImage();
		if (image != null && !image.isDisposed()) {
			event.gc.drawImage(image, 0, 0);
		}
	}
	
	private void disposeImage() {
		if (image != null) {
			image.dispose();
			image = null;
		}
	}
	
	private void updateImage() {
		if (image == null) {
			Point size = getSize();
			image = renderFormula(descriptor, size, backgroundColor);
		}
	}
	
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		if (hHint == SWT.DEFAULT) {
			hHint = 40;
		}
		if (wHint == SWT.DEFAULT) {
			wHint = 200;
		}
		return super.computeSize(wHint, hHint, changed);
	}
	
}
