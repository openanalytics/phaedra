package eu.openanalytics.phaedra.ui.wellimage.canvas;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.swt.graphics.Point;

import eu.openanalytics.phaedra.model.plate.vo.Well;

public class CanvasState implements Cloneable {

	private float scale;
	private Point offset;
	
	private Well well;
	private Point fullImageSize;
	private boolean[] channels;

	private boolean forceChange;

	public CanvasState() {
		clearState();
	}
	
	public float getScale() {
		return scale;
	}
	
	public Point getOffset() {
		return offset;
	}
	
	public Well getWell() {
		return well;
	}
	
	public Point getFullImageSize() {
		return fullImageSize;
	}
	
	public boolean[] getChannels() {
		return channels;
	}
	
	public boolean isForceChange() {
		return forceChange;
	}
	
	public void setScale(float scale) {
		this.scale = scale;
	}
	
	public void setOffset(Point offset) {
		this.offset = offset;
	}
	
	public void setWell(Well well, Point fullImageSize) {
		this.well = well;
		this.fullImageSize = fullImageSize;
	}
	
	public void setChannels(boolean[] channels) {
		this.channels = channels;
	}
	
	public void setForceChange(boolean forceChange) {
		this.forceChange = forceChange;
	}
	
	public void clearState() {
		scale = 1.0f;
		offset = new Point(0, 0);
		well = null;
		fullImageSize = new Point(0, 0);
		channels = null;
		forceChange = false;
	}
	
	public boolean scaleChanged(CanvasState otherState) {
		return otherState == null || scale != otherState.scale;
	}
	
	public boolean offsetChanged(CanvasState otherState) {
		return !Objects.equals(offset, otherState.offset);
	}

	public boolean wellChanged(CanvasState otherState) {
		return !Objects.equals(well, otherState.well);
	}

	public boolean channelsChanged(CanvasState otherState) {
		return otherState == null || !Arrays.equals(channels, otherState.channels);
	}
	
	public boolean anyChanged(CanvasState otherState) {
		return isForceChange() || scaleChanged(otherState) || offsetChanged(otherState) || wellChanged(otherState) || channelsChanged(otherState);
	}
	
	public CanvasState copy() {
		try {
			return CanvasState.class.cast(clone());
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	@Override
	public String toString() {
		return String.format("CanvasState [sc: %f] [off: %s] [well: %s size: %s] [ch: %s] [dirty: %b]", scale, offset, well, fullImageSize, Arrays.toString(channels), forceChange);
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		CanvasState clone = CanvasState.class.cast(super.clone());
		if (channels != null) clone.channels = Arrays.copyOf(channels, channels.length);
		if (fullImageSize != null) clone.fullImageSize = new Point(fullImageSize.x, fullImageSize.y);
		if (offset != null) clone.offset = new Point(offset.x, offset.y);
		return clone;
	}
}
