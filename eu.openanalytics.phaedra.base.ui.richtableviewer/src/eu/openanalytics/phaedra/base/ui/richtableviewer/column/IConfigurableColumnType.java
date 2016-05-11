package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.richtableviewer.Activator;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;

public interface IConfigurableColumnType {
	
	public static final String EXT_PT_ID = Activator.PLUGIN_ID + ".configurableColumnType";
	public static final String ATTR_CLASS = "class";

	public void fillConfigArea(Composite parent, RichTableViewer tableViewer);
	
	public ColumnDataType getColumnDataType();

	public String getName();
	
}