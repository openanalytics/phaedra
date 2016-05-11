package eu.openanalytics.phaedra.export.core.filter;

import java.util.List;

import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class WellFeatureFilter {

	public final static String[] OPERATORS = {"<",">","=","<=",">="};
	public final static String NORM_NONE = "NONE";
	
	private List<Feature> features;
	
	public WellFeatureFilter(List<Feature> features) {
		this.features = features;
	}
	
	public String[] getFeatureNames() {
		String[] names = new String[features.size()];
		for (int i=0; i<names.length; i++) {
			names[i] = features.get(i).getDisplayName();
		}
		return names;
	}
	
	public String[] getNormalizations(int index) {
		if (index == -1) return new String[]{NORM_NONE};
		
		Feature f = features.get(index);
		String curveNormalization = f.getNormalization();
		if (NORM_NONE.equals(curveNormalization)) {
			return new String[]{NORM_NONE};
		} else {
			return new String[]{curveNormalization, NORM_NONE};
		}
	}
	
	public String[] getOperators() {
		return OPERATORS;
	}
	
	public Feature getFeature(int i) {
		return features.get(i);
	}
}
