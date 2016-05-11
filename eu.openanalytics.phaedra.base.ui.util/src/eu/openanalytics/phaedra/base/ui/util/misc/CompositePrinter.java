package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * The purpose of this class is to provide an "invisible" Composite
 * which can be filled with content and then printed onto an Image.
 *
 * If desired, the size and layout of the container Composite
 * can be altered during fillComposite(Composite container);
 */
public abstract class CompositePrinter {

	private Shell shell;
	private Control control;

	private Composite oldParent;
	private Composite container;

	public CompositePrinter(int w, int h) {
		shell = new Shell();
		container = new Composite(shell, SWT.NONE);

		// DO NOT REMOVE, this fixes the screen flickering.
		// TODO: Confirm this fix works on other PC's as well
		container.setLocation(250, 250);

		container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		GridLayoutFactory.fillDefaults().margins(0,0).applyTo(container);
		container.setSize(w, h);
	}

	/**
	 * Fill the container with controls.
	 * By default, the container has a 1-column grid layout.
	 * Please make sure to use the setParent(Control control) method to change the parent
	 */
	protected abstract void fillComposite(Composite container);

	public Image generateImage() {
		Image image = null;
		GC gc = null;
		try {
			fillComposite(container);
			container.layout();
			Point size = container.getSize();
			image = new Image(null, size.x, size.y);
			gc = new GC(image);

			int attempts = 0;
			do {
				try {
					container.print(gc);
					Thread.sleep(150);
					attempts++;
					Display.getDefault().readAndDispatch();
				} catch (InterruptedException e) {}
			}
			// If render is still in progress after print(gc) returns, it's an async rendering.
			while (isRenderInProgress() && attempts < 50);

			container.print(gc);
		} finally {
			if (gc != null && !gc.isDisposed()) gc.dispose();
			dispose();
		}
		return image;
	}

	public void generateImage(String destinationPath, int imageType) {
		Image image = null;
		try {
			image = generateImage();
			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] { image.getImageData() };
			loader.save(destinationPath, imageType);
		} finally {
			if (image != null && !image.isDisposed()) image.dispose();
		}
	}

	/**
	 * Set the invisible container as parent for the given Control.
	 * @param control The control for which to change the parent
	 */
	protected void setParent(Control control) {
		this.control = control;
		oldParent = control.getParent();
		control.setParent(container);
	}

	/**
	 * If rendering is async (e.g. multithreaded), this method can
	 * be overridden to tell the printer when the rendering is complete.
	 *
	 * As long as it returns false, the generateImage method tries
	 * to print again with a small delay.
	 */
	protected boolean isRenderInProgress() {
		return false;
	}

	private void dispose() {
		if (control != null && !control.isDisposed()&& oldParent != null && !oldParent.isDisposed()) control.setParent(oldParent);
		if (container != null && !container.isDisposed()) container.dispose();
		if (shell != null && !shell.isDisposed()) shell.dispose();
	}
}
