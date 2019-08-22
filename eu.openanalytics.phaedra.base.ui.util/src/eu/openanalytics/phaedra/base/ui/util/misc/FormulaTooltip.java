package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

import com.google.common.base.Objects;

import eu.openanalytics.phaedra.base.util.misc.FormulaDescriptor;


public class FormulaTooltip extends ToolTip implements IFormulaRenderer {
	
	
	private Control control;
	
	private FormulaDescriptor descriptor;
	
	private Label display;
	
	private Image image;
	
	
	public FormulaTooltip(Control control) {
		super(control, ToolTip.NO_RECREATE, false);
		this.control = control;
		
		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				disposeImage();
			}
		});
	}
	
	
	public void setFormula(FormulaDescriptor descriptor) {
		if (Objects.equal(this.descriptor, descriptor)) {
			return;
		}
		this.descriptor = descriptor;
		
		disposeImage();
		
		if (display == null || display.isDisposed()) {
			return;
		}
		updateImage();
		if (image != null) {
			display.getShell().pack(true);
			if (!display.getShell().isVisible()) display.getShell().setVisible(true);
		}
		else {
			if (display.getShell().isVisible()) display.getShell().setVisible(false);
		}
	}
	
	
	@Override
	protected boolean shouldCreateToolTip(Event event) {
		return (super.shouldCreateToolTip(event) && canRenderFormula(descriptor));
	}
	
	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(event.widget.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		GridLayoutFactory.swtDefaults().applyTo(composite);
		
		display = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(display);
		
		updateImage();
		
		return composite;
	}
	
	@Override
	public Point getLocation(Point tipSize, Event event) {
		Rectangle bounds = control.getBounds();
		Point point = control.getParent().toDisplay(bounds.x + bounds.width + 3, bounds.y);
		if (control.getMonitor().getBounds().contains(point.x + 500, point.y)) {
			return point;
		}
		return super.getLocation(tipSize, event);
	}
	
	@Override
	protected void afterHideToolTip(Event event) {
		this.display = null;
		super.afterHideToolTip(event);
	}
	
	private void updateImage() {
		if (image == null) {
			Point size = new Point(500, 100);
			image = renderFormula(descriptor, size, display.getParent().getBackground());
		}
		display.setImage(image);
	}
	
	private void disposeImage() {
		if (image != null) {
			image.dispose();
			image = null;
		}
	}
	
}
