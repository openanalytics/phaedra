package eu.openanalytics.phaedra.datacapture.montage.layout;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;

import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.montage.MontageConfig;

public class BaseFieldLayoutSource implements IFieldLayoutSource, IExecutableExtension {

	private String id;
	
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		id = config.getAttribute(IFieldLayoutSource.ATTR_ID);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public FieldLayout getLayout(PlateReading reading, int fieldCount, MontageConfig montageConfig, DataCaptureContext context) {
		// Default behaviour: return null.
		return null;
	}

}
