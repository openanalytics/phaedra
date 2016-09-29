package eu.openanalytics.phaedra.model.curve.osb;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.curve.ICurveFitModel;
import eu.openanalytics.phaedra.model.curve.ICurveFitModelFactory;

public class OSBFitModelFactory implements ICurveFitModelFactory {

	private static final String[] MODEL_IDS = {
			"PL2", "PL3L", "PL3U", "PL4",
			"PL2_R", "PL3L_R", "PL3U_R", "PL4_R",
			"PL2H1", "PL3LH1", "PL3UH1", "PL4H1",
			"PL2H1_R", "PL3LH1_R", "PL3UH1_R", "PL4H1_R",
			"PLOTONLY"
	};
	
	@Override
	public String[] getSupportedModelIds() {
		return MODEL_IDS;
	}

	@Override
	public ICurveFitModel createModel(String id) {
		if (CollectionUtils.contains(MODEL_IDS, id)) return new OSBFitModel(id);
		else return null;
	}

}
