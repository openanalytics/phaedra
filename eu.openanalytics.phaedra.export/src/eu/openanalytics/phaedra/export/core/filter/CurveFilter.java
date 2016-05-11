package eu.openanalytics.phaedra.export.core.filter;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CurveFilter {

	public final static String[] OPERATORS = {"<",">","=","<=",">="};
	
	private List<Feature> features;
	
	public CurveFilter(List<Feature> features) {
		List<Feature> validFeatures = new ArrayList<>();
		for (Feature f: features) {
			if (f.getCurveSettings().get(CurveSettings.KIND) == null) continue;
			validFeatures.add(f);
		}
		this.features = validFeatures;
	}
	
	public String[] getFeatureCurves() {
		List<String> curves = new ArrayList<String>();
		for (int i=0; i<features.size(); i++) {
			Feature f = features.get(i);
			String prop = null;
			String kind = f.getCurveSettings().get(CurveSettings.KIND);
			if ("OSB".equals(kind)) {
				prop = "pIC50";
			} else if ("PLAC".equals(kind)) {
				prop = "pLAC";
			}
			if (prop != null) {
				curves.add(f.getDisplayName() + " (" + f.getNormalization() + ") " + prop);
			}
		}
		return curves.toArray(new String[curves.size()]);
	}

	public String[] getOperators() {
		return OPERATORS;
	}
	
	public Feature getFeature(int i) {
		return features.get(i);
	}
}
