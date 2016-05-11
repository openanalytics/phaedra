package eu.openanalytics.phaedra.base.ui.util.copy;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

public interface ICopyable {
	
	public Point getCopySize();
	
	public void copy(GC gc);
	
}
