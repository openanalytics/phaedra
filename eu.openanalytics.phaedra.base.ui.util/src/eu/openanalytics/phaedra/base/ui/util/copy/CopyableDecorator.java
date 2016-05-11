package eu.openanalytics.phaedra.base.ui.util.copy;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.util.view.PartDecorator;

/**
 * A decorator that makes the part copyable. This means the contents of the part can be copied to a GC, like a screenshot.
 */
public class CopyableDecorator extends PartDecorator implements ICopyable {

	private Composite compositeToCopy;
	
	@Override
	public void onCreate(Composite parent) {
		compositeToCopy = parent;
	}
	
	@Override
	public void onDispose() {
		// Do nothing.
	}
	
	@Override
	public void contributeContextMenu(IMenuManager manager) {
		// Do nothing.
	}
	
	@Override
	public Point getCopySize() {
		if (compositeToCopy == null) return new Point(0,0);
		return compositeToCopy.getSize();
	}
	
	@Override
	public void copy(GC gc) {
		if (compositeToCopy == null) return;
		compositeToCopy.print(gc);
	}
}
