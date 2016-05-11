package eu.openanalytics.phaedra.base.imaging.overlay;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Canvas;

public class ImageOverlayFactory {

	public static IImageOverlay create(Canvas canvas, String id) {
		final IImageOverlay overlay = instantiateOverlay(id);
		
		if (overlay instanceof CanvasImageOverlay) {
			((CanvasImageOverlay)overlay).setCanvas(canvas);
		}
		
		// Attach paint listener to canvas.
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				overlay.render(e.gc);
			}
		});
		canvas.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				overlay.dispose();
			}
		});
		
		// Attach mouse listeners to canvas (optional).
		MouseListener ml = overlay.getMouseListener();
		if (ml != null) canvas.addMouseListener(ml);
		MouseMoveListener mml = overlay.getMouseMoveListener();
		if (mml != null) canvas.addMouseMoveListener(mml);
		KeyListener kl = overlay.getKeyListener();
		if(kl != null) canvas.addKeyListener(kl);
		
		return overlay;
	}
	
	private static IImageOverlay instantiateOverlay(String id) {
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IImageOverlay.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				String overlayId = el.getAttribute(IImageOverlay.ATTR_ID);
				if (id.equals(overlayId)) {
					Object o = el.createExecutableExtension(IImageOverlay.ATTR_CLASS);
					if (o instanceof IImageOverlay) {
						IImageOverlay overlay = (IImageOverlay)o;
						return overlay;
					}
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
		return null;
	}
}
