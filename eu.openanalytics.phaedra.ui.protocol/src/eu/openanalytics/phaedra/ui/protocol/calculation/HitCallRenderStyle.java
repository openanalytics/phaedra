package eu.openanalytics.phaedra.ui.protocol.calculation;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;

public enum HitCallRenderStyle {

	CheckMark(0),
	Star(1),
	Diamond(2);

	private int code;
	private Image image;
	
	private HitCallRenderStyle(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	public Image getImage() {
		if (image == null) image = createImage();
		return image;
	}
	
	public Image createImage() {
		Image img = new Image(null, 15, 15);
		GC gc = new GC(img);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		renderImage(gc, 7, 7, 5);
		gc.dispose();
		return img;
	}
	
	public void renderImage(GC gc, int x, int y, int size) {
		PlotShape.valueOf(this.name()).drawShape(gc, x, y, size);
	}
	
	public static HitCallRenderStyle getByCode(int code) {
		for (HitCallRenderStyle style: values()) {
			if (style.code == code) return style;
		}
		return null;
	}
}
