package eu.openanalytics.phaedra.ui.subwell.wellimage.edit;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;

import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;

public interface IPaletteTool extends ISelectionChangedListener {

	public String getLabel();
	
	public void restoreState(IMemento memento);
	
	public void saveState(IMemento memento);
	
	public JP2KOverlay[] configureOverlays(IPaletteToolHost host);
	
	public void createUI(Composite parent);
	
	public void start();
	
	public void add(PathData path, ISelection selection);
	
	public void update();
	
	public void cancel();
	
	public void finish();
	
	public void dispose();
}
