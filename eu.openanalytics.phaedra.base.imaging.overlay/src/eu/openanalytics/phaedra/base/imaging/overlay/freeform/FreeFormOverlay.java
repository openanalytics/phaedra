package eu.openanalytics.phaedra.base.imaging.overlay.freeform;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Transform;

import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;
import eu.openanalytics.phaedra.base.imaging.overlay.freeform.event.FreeFormEventManager;
import eu.openanalytics.phaedra.base.imaging.overlay.freeform.event.FreeFormListener;
import eu.openanalytics.phaedra.base.imaging.overlay.freeform.impl.LineProvider;
import eu.openanalytics.phaedra.base.imaging.overlay.freeform.impl.PolygonProvider;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;

public class FreeFormOverlay extends JP2KOverlay {

	private MouseListener mouseListener;
	private MouseMoveListener mouseMoveListener;
	private KeyListener keyListener;
	
	private FreeFormEventManager eventManager;

	private List<IFreeFormProvider> formProviders;
	private IFreeFormProvider activeProvider;
	
	public FreeFormOverlay() {
		eventManager = new FreeFormEventManager();
		eventManager.addListener(new FreeFormListener(){
			@Override
			public void shapeResumed(int x, int y) {
				getCanvas().redraw();
			}
			@Override
			public void shapeFinished(PathData pathData) {
				getCanvas().redraw();
			}
		});

		formProviders = new ArrayList<>();
		
		IPointTranslator pointTranslator = new IPointTranslator() {
			@Override
			public Point screenToImage(int x, int y) {
				return getImageCoords(new Point(x, y));
			}
			@Override
			public Point imageToScreen(int x, int y) {
				return translate(new Point(x, y));
			}
		};
		
		PolygonProvider polygonProvider = new PolygonProvider();
		polygonProvider.setEventManager(eventManager);
		polygonProvider.setPointTranslator(pointTranslator);
		formProviders.add(polygonProvider);
		
		LineProvider lineProvider = new LineProvider();
		lineProvider.setEventManager(eventManager);
		lineProvider.setPointTranslator(pointTranslator);
		formProviders.add(lineProvider);
		
		mouseListener = new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (activeProvider != null) activeProvider.onMouseDown(e.x, e.y);
			}
			@Override
			public void mouseUp(MouseEvent e) {
				if (activeProvider != null) activeProvider.onMouseUp(e.x, e.y);
			}
		};
		
		mouseMoveListener = new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if (activeProvider != null) activeProvider.onMouseMove(e.x, e.y);
			}
		};
		
		keyListener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (activeProvider != null) activeProvider.onKeyPress(e.keyCode);
			}
			@Override
			public void keyReleased(KeyEvent e) {
				if (activeProvider != null) activeProvider.onKeyRelease(e.keyCode);
			}
		};
	}
	
	public IFreeFormProvider[] getFormProviders() {
		return formProviders.toArray(new IFreeFormProvider[formProviders.size()]);
	}
	
	/**
	 * Enable the drawing of freeforms.
	 */
	public void enable(IFreeFormProvider p) {
		if (formProviders.contains(p)) activeProvider = p;
	}
	
	/**
	 * Disable the drawing of freeforms.
	 */
	public void disable() {
		activeProvider = null;
	}
	
	/**
	 * Undo (remove) the most recently drawn freeform.
	 */
	public void undo() {
		if (activeProvider == null) return;
		activeProvider.undo();
		getCanvas().redraw();
	}
	
	/**
	 * Reset (remove) all drawn freeforms.
	 */
	public void reset() {
		if (activeProvider == null) return;
		activeProvider.reset();
		getCanvas().redraw();
	}
	
	public void addFreeFormListener(FreeFormListener listener) {
		eventManager.addListener(listener);
	}
	
	public void removeFreeFormListener(FreeFormListener listener) {
		eventManager.removeListener(listener);
	}
	
	@Override
	public MouseListener getMouseListener() {
		return mouseListener;
	}

	@Override
	public MouseMoveListener getMouseMoveListener() {
		return mouseMoveListener;
	}
	
	@Override
	public KeyListener getKeyListener() {
		return keyListener;
	}
	
	@Override
	public boolean overridesMouseEvents(int x, int y) {
		return (activeProvider != null);
	}
	
	@Override
	public void render(GC gc) {
		drawForms(getScale(), getOffset().x, getOffset().y, gc, false, 0);
	}
	
	@Override
	public void dispose() {
		super.dispose();
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	protected float getSize(PathData path) {
		if (activeProvider == null) return 0f;
		if (activeProvider instanceof LineProvider) return SWTUtils.getLength(path);
		if (activeProvider instanceof PolygonProvider) return SWTUtils.getSurface(path);
		return 0f;
	}
	
	protected ImageData getAsLabelImage(int w, int h, int startingLabel) {
		// Draw a label image.
		Image img = new Image(null, w, h);
		GC gc = new GC(img);
		drawForms(1.0f, 0f, 0f, gc, true, startingLabel);
		gc.dispose();
		ImageData data = img.getImageData();
		img.dispose();

		// A new Image by default is 32bit (on Windows). For label images, convert it to 8bit.
		data = to8bit(data);
		return data;
	}
	
	private void drawForms(float scale, float offsetX, float offsetY, GC gc, boolean labelImg, int startingLabel) {
		if (activeProvider == null) return;
		Transform t = new Transform(gc.getDevice());
		t.scale(scale, scale);
		t.translate(-offsetX, -offsetY);
		gc.setTransform(t);
		activeProvider.draw(gc, labelImg, startingLabel);
		t.dispose();
	}
	
	public static ImageData to8bit(ImageData data) {
		if (data.depth == 8) return data;

		// Convert to 8bit greyscale.
		byte[] newBuffer = new byte[data.width*data.height];

		for (int x=0; x<data.width; x++) {
			for (int y=0; y<data.height; y++) {
				int pixel = data.getPixel(x, y);
				int shift = data.depth == 32 ? 8 : 0;
				int r = (pixel>>(16+shift)) & 0xFF;
				int g = (pixel>>(8+shift)) & 0xFF;
				int b = (pixel>>(shift)) & 0xFF;
				
				int a = 255;
				if (data.depth == 32) a = pixel & 0xFF;
				
				// By ignoring alpha 0, we skip the image background.
				if (a > 0 && (r != 0 || g != 0 || b != 0)) {
					int i = x + y*data.width;
					newBuffer[i] = (byte)r;
				}
			}
		}

		// Make a palette for the 8bit image.
		int colorCount = 256;
		RGB[] rgbs = new RGB[colorCount];
		for (int i = 0; i < colorCount; i++) {
			int value = i * 0xFF / (colorCount - 1);
			rgbs[i] = new RGB(value, value, value);
		}
		PaletteData palette = new PaletteData(rgbs);
		data = new ImageData(data.width, data.height, 8, palette, 1, newBuffer);
		return data;
	}
}
