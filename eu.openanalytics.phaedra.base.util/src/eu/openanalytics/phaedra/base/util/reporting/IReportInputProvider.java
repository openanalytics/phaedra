package eu.openanalytics.phaedra.base.util.reporting;

import java.util.List;
import java.util.Set;

public interface IReportInputProvider {

	public Set<String> getDataProviderNames();
	
	public List<?> getDataProvider(String type);
	
	public void setDataProvider(String type, List<?> dataProviders);

	public void setDataProvider(String type, Object singleObject);
	
}
