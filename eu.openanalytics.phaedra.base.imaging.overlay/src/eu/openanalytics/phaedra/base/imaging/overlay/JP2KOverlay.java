package eu.openanalytics.phaedra.base.imaging.overlay;

import java.util.Map;

import org.eclipse.swt.graphics.Point;


public abstract class JP2KOverlay extends CanvasImageOverlay {

	private float scale;
	private Point offset;
	
	public JP2KOverlay() {
		this.scale = 1.0f;
		this.offset = new Point(0,0);
	}

	public void setScale(float scale) {
		this.scale = scale;
	}
	
	public void setOffset(Point offset) {
		this.offset = offset;
	}
	
	protected float getScale() {
		return scale;
	}
	
	protected Point getOffset() {
		return offset;
	}
	
	protected Point translate(Point p) {
		// Image point to screen point.
		int x = (int)(scale*(p.x - offset.x));
		int y = (int)(scale*(p.y - offset.y));
		return new Point(x,y);
	}
	
	protected Point getImageCoords(Point p) {
		// Screen point to image point.
		int x = offset.x + (int)(p.x / scale);
		int y = offset.y + (int)(p.y / scale);
		return new Point(x,y);
	}
	
	public void applySettingsMap(Map<String, Object> settingsMap) {
		// Default: no settings.
	}
	
	public Map<String, Object> createSettingsMap() {
		// Default: no settings.
		return null;
	}
}
