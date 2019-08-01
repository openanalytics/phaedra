package eu.openanalytics.phaedra.model.curve;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;

import eu.openanalytics.phaedra.model.curve.CurveParameter.Definition;


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
	
	
	@Override
	public List<Definition> getOutputParameters(CurveFitSettings fitSettings) {
		return Arrays.asList(getOutputParameters());
	}
	@Override
	public List<Definition> getOutputKeyParameters() {
		return getOutputParameters(null).stream().filter((def) -> def.key).collect(Collectors.toList());
	}
	
	@Deprecated
	/** For backward compatibility only */
	public Definition[] getOutputParameters() {
		throw new UnsupportedOperationException();
	}
	
}
