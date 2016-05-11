package eu.openanalytics.phaedra.datacapture.scanner;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;

import eu.openanalytics.phaedra.datacapture.scanner.model.IScannerType;

public abstract class BaseScannerType implements IScannerType, IExecutableExtension {

	private String id;
	
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		this.id = config.getAttribute(IScannerType.ATTR_ID);
	}

	@Override
	public String getId() {
		return id;
	}

}
