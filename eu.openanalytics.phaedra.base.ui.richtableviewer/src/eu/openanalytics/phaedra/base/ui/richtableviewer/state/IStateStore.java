package eu.openanalytics.phaedra.base.ui.richtableviewer.state;

import java.io.IOException;

import eu.openanalytics.phaedra.base.ui.richtableviewer.Activator;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;

public interface IStateStore {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".stateStore";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	
	public ColumnConfiguration[] loadState(String tableKey) throws IOException;
	
	public void saveState(ColumnConfiguration[] configs, String tableKey) throws IOException;
}
