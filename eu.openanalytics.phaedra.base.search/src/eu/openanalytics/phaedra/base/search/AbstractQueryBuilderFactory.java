package eu.openanalytics.phaedra.base.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public abstract class AbstractQueryBuilderFactory implements IQueryBuilderFactory {
	private String id;
	
	@Override
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		this.id = config.getAttribute(IQueryBuilderFactory.ATTR_ID);
	}
	
}
