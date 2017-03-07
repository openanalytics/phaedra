package eu.openanalytics.phaedra.base.ui.util.copy.extension;

import eu.openanalytics.phaedra.base.ui.util.Activator;

public interface ICopyTextProvider {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".copyTextProvider";
	public final static String ATTR_CLASS = "class";

	public boolean isValidWidget(Object widget);

	public abstract Object getValueToCopy(Object widget);

}
