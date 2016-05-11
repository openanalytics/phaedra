package eu.openanalytics.phaedra.base.ui.util.text;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

/**
 * Convenience enumeration for Info/Warning/Error annotations.
 */
public enum AnnotationStyle {

	Info(new TextStyle(), IconManager.getIconImage("information.png")),
	Warning(new TextStyle(), IconManager.getIconImage("error.png")),
	Error(new TextStyle(), IconManager.getIconImage("exclamation.png"));
	
	static {
		Info.getStyle().underline = true;
		Info.getStyle().underlineStyle = SWT.UNDERLINE_SQUIGGLE;
		Info.getStyle().underlineColor = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);

		Warning.getStyle().underline = true;
		Warning.getStyle().underlineStyle = SWT.UNDERLINE_SQUIGGLE;
		Warning.getStyle().underlineColor = new Color(Display.getDefault(), 180, 230, 0);

		Error.getStyle().underline = true;
		Error.getStyle().underlineStyle = SWT.UNDERLINE_SQUIGGLE;
		Error.getStyle().underlineColor = Display.getDefault().getSystemColor(SWT.COLOR_RED);
	}
	
	private TextStyle style;
	private Image image;
	
	private AnnotationStyle(TextStyle style, Image image) {
		this.style = style;
		this.image = image;
	}
	
	public TextStyle getStyle() {
		return style;
	}
	
	public Image getImage() {
		return image;
	}
	
	public TextAnnotation create(int start, int end, String text) {
		TextAnnotation annotation = new TextAnnotation();
		annotation.setStart(start);
		annotation.setEnd(end);
		annotation.setText(text);
		annotation.setStyle(style);
		annotation.setImage(image);
		return annotation;
	}
}
