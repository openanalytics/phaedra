package eu.openanalytics.phaedra.base.ui.charting.v2.data;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;

public class DataProviderSettings<ENTITY, ITEM> implements Serializable {

	private static final long serialVersionUID = 4020289386163866824L;

	private List<String> features;
	private List<String> auxiliaryFeatures;
	private Map<String, Object> filterProperties;
	private transient IGroupingStrategy<ENTITY, ITEM> groupingStrategy;
	private String groupingStrategyClassName;
	private String[] jepExpressions;
	private String[] axisLabels;
	private String title;
	private String aggregationMethod;
	private String aggregationFeature;

	private Map<String, String> miscSettings;

	public DataProviderSettings() {
		features 			= new ArrayList<String>();
		auxiliaryFeatures 	= new ArrayList<String>();
		groupingStrategy	= new DefaultGroupingStrategy<ENTITY, ITEM>();
		jepExpressions 		= new String[] {"","","","","",""};
		aggregationMethod	= AggregationDataCalculator.NONE;
		aggregationFeature	= AggregationDataCalculator.NONE;
		miscSettings		= new HashMap<String, String>();
	}

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		// Support for older saved views.
		if (miscSettings == null) miscSettings = new HashMap<>();
		for (int i = 0; i < features.size(); i++) {
			if (features.get(i).equals("enter expr.")) {
				features.set(i, BaseDataProvider.EXPRESSIONSTRING);
			}
		}
	}

	public String getFeature(int dimension) {
		if (features != null && dimension < features.size()) {
			return features.get(dimension);
		}
		return null;
	}

	public String getAuxiliaryFeature(int dimension) {
		if (auxiliaryFeatures != null && dimension < auxiliaryFeatures.size()) {
			return auxiliaryFeatures.get(dimension);
		}
		return null;
	}

	public List<String> getFeatures() {
		return features;
	}

	public void setFeatures(List<String> features) {
		this.features = features;
	}

	public List<String> getAuxiliaryFeatures() {
		return auxiliaryFeatures;
	}

	public void setAuxiliaryFeatures(List<String> auxiliaryFeatures) {
		this.auxiliaryFeatures = auxiliaryFeatures;
	}

	public Map<String, Object> getFilterProperties() {
		return filterProperties;
	}

	public void setFilterProperties(Map<String, Object> filterProperties) {
		this.filterProperties = filterProperties;
	}

	public IGroupingStrategy<ENTITY, ITEM> getGroupingStrategy() {
		if (groupingStrategy == null) {
			groupingStrategy = getGroupingStategyByClassName();
		}
		return groupingStrategy;
	}

	public void setGroupingStrategy(IGroupingStrategy<ENTITY, ITEM> groupingStategy) {
		this.groupingStrategy = groupingStategy;
		this.groupingStrategyClassName = groupingStategy.getClass().getName();
	}

	@SuppressWarnings("unchecked")
	public IGroupingStrategy<ENTITY, ITEM> getGroupingStategyByClassName() {
		try {
			Class<?> classGroup = Class.forName(groupingStrategyClassName);
			return (IGroupingStrategy<ENTITY, ITEM>) classGroup.newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			return new DefaultGroupingStrategy<ENTITY, ITEM>();
		}
	}

	public String getGroupingStrategyClassName() {
		return groupingStrategyClassName;
	}

	public String[] getJepExpressions() {
		return jepExpressions;
	}

	public void setJepExpressions(String[] jepExpressions) {
		this.jepExpressions = jepExpressions;
	}

	public String[] getAxisLabels() {
		return axisLabels;
	}

	public void setAxisLabels(String[] axisLabels) {
		this.axisLabels = axisLabels;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAggregationMethod() {
		return aggregationMethod;
	}

	public void setAggregationMethod(String aggregationMethod) {
		this.aggregationMethod = aggregationMethod;
	}

	public String getAggregationFeature() {
		return aggregationFeature;
	}

	public void setAggregationFeature(String aggregationFeature) {
		this.aggregationFeature = aggregationFeature;
	}

	/*
	 * Misc settings
	 * *************
	 */

	public Map<String, String> getMiscSettings() {
		return miscSettings;
	}

	public void setMiscSetting(String name, String value) {
		miscSettings.put(name, value);
	}

}