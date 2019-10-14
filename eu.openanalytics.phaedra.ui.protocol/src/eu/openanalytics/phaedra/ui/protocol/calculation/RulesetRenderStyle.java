package eu.openanalytics.phaedra.ui.protocol.calculation;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;

public enum RulesetRenderStyle {

	CheckMark(0),
	Star(1),
	Diamond(2),
	Ellipse(3),
	Rectangle(4),
	TriangleUp(5),
	TriangleRight(6),
	TriangleLeft(7),
	TriangleDown(8),
	Cross(9),
	DiagonalCross(10),
	ProhibitorySign(11),
	QuestionMark(12);
		
	private int code;
	private Image image;
	
	private RulesetRenderStyle(int code) {
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
	
	public static RulesetRenderStyle getByCode(int code) {
		for (RulesetRenderStyle style: values()) {
			if (style.code == code) return style;
		}
		return null;
	}
}
