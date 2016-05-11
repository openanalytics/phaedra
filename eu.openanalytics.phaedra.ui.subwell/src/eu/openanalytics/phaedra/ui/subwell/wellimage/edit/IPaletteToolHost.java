package eu.openanalytics.phaedra.ui.subwell.wellimage.edit;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.ui.wellimage.util.JP2KImageCanvas;

public interface IPaletteToolHost {

	public JP2KImageCanvas getCanvas();
	
	public IValueObject getInputObject();
	
	public void toggleDrawMode(boolean enabled);
	
	public boolean isDirty();
	
	public void setDirty(boolean dirty);
}
