package eu.openanalytics.phaedra.model.curve;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;

public abstract class AbstractCurveFitModel implements ICurveFitModel, IExecutableExtension {

	private String id;
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		this.id = config.getAttribute(ICurveFitModel.ATTR_ID);
	}
}
