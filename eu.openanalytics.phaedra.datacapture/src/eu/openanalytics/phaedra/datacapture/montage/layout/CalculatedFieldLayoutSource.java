package eu.openanalytics.phaedra.datacapture.montage.layout;

import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.montage.MontageConfig;
import eu.openanalytics.phaedra.datacapture.util.VariableResolver;

public class CalculatedFieldLayoutSource extends LiteralFieldLayoutSource {

	private final static String PARAM_NAME = "reading.fieldLayout";
	
	@Override
	public FieldLayout getLayout(PlateReading reading, int fieldCount, MontageConfig montageConfig, DataCaptureContext context) {
		Object fieldLayout = VariableResolver.get(PARAM_NAME, context);
		if (fieldLayout instanceof FieldLayout) {
			return (FieldLayout)fieldLayout;
		} else if (fieldLayout instanceof String) {
			montageConfig.layout = (String)fieldLayout;
			return super.getLayout(reading, fieldCount, montageConfig, context);
		}
		throw new RuntimeException("Cannot load field layout: no layout specified in parameter " + PARAM_NAME);
	}

}
