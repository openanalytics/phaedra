package eu.openanalytics.phaedra.base.ui.util.text;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;

public class TextAnnotation {

	private int start;
	private int end;
	private TextStyle style;
	private String text;
	private Image image;
	
	public TextAnnotation() {
		// Default constructor
	}
	
	public TextAnnotation(int start, int end, TextStyle style, String text, Image image) {
		this.start = start;
		this.end = end;
		this.style = style;
		this.text = text;
		this.image = image;
	}
	
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public TextStyle getStyle() {
		return style;
	}
	public void setStyle(TextStyle style) {
		this.style = style;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Image getImage() {
		return image;
	}
	public void setImage(Image image) {
		this.image = image;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + end;
		result = prime * result + start;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TextAnnotation other = (TextAnnotation) obj;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}
}
