package eu.openanalytics.phaedra.base.imaging.overlay;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.ISelectionListener;

public interface IImageOverlay {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".imageOverlay";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	
	public void render(GC gc);

	public void setCurrentMouseMode(int currentMouseMode);
	public boolean overridesMouseEvents(int x, int y);
	
	public MouseListener getMouseListener();
	public MouseMoveListener getMouseMoveListener();
	public KeyListener getKeyListener();
	
	public void createButtons(ToolBar parent);
	public void createContextMenu(IMenuManager manager);
	
	public ISelectionListener[] getSelectionListeners();
	public ISelectionProvider getSelectionProvider();
	
	public void dispose();
}
