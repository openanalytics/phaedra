package eu.openanalytics.phaedra.ui.plate.grid.layer.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class MultiFeatureConfig implements Serializable {
	
	private static final long serialVersionUID = -8332506411425434305L;
	
	public long[] featureIds;
	public boolean fixedPieSize;
	
	public MultiFeatureConfig() {
		featureIds = null;
		fixedPieSize = false;
	}
	
	public MultiFeatureConfig(List<Feature> features) {
		getFeatureId(features);
	}
	
	public MultiFeatureConfig(List<Feature> features, boolean fixedPieSize) {
		getFeatureId(features);
		this.fixedPieSize = fixedPieSize;
	}

	private void getFeatureId(List<Feature> features) {
		if (features == null) return;
		this.featureIds = new long[features.size()];
		for (int i = 0; i < features.size(); i++) {
			if (features.get(i) != null) {
				featureIds[i] = features.get(i).getId();
			} else {
				featureIds[i] = -1;
			}
		}
	}

	public List<Feature> getFeatures() {
		List<Feature> features = new ArrayList<>();
		for (long featureId : featureIds) {
			if (featureId > 0) {
				features.add(ProtocolService.getInstance().getFeature(featureId));
			} else {
				features.add(null);
			}
		}
		return features;
	}
	
}
