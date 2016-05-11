package eu.openanalytics.phaedra.base.ui.navigator.providers;

import eu.openanalytics.phaedra.base.ui.navigator.Activator;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;

public interface IElementProvider {

	public final static String EXT_POINT_ID = Activator.PLUGIN_ID + ".elementProvider";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_USER_MODE = "userMode";
	
	public IElement[] getChildren(IGroup parent);
}
