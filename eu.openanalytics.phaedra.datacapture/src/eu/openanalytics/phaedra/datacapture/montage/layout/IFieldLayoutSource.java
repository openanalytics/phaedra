package eu.openanalytics.phaedra.datacapture.montage.layout;

import eu.openanalytics.phaedra.datacapture.Activator;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.montage.MontageConfig;

public interface IFieldLayoutSource {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".fieldLayoutSource";
	public final static String ATTR_ID = "id";
	public final static String ATTR_CLASS = "class";
	
	public String getId();
	
	public FieldLayout getLayout(PlateReading reading, int fieldCount, MontageConfig montageConfig, DataCaptureContext context);
}
