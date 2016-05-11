package eu.openanalytics.phaedra.base.util.reporting;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportInputProvider implements IReportInputProvider {

	private Map<String, List<?>> dataMap = new HashMap<String, List<?>>();

	@Override
	public Set<String> getDataProviderNames() {
		return dataMap.keySet();
	}

	@Override
	public List<?> getDataProvider(String type) {
		return dataMap.get(type);
	}

	@Override
	public void setDataProvider(String type, List<?> dataProviders) {
		dataMap.put(type, dataProviders);
	}

	@Override
	public void setDataProvider(String type, Object singleObject) {
		dataMap.put(type, Arrays.asList(singleObject));
	}

}