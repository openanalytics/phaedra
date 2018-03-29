package eu.openanalytics.phaedra.model.curve.osb;

import java.util.stream.IntStream;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.curve.ICurveFitModel;
import eu.openanalytics.phaedra.model.curve.ICurveFitModelFactory;

public class OSBFitModelFactory implements ICurveFitModelFactory {

	public static final String[] MODEL_IDS = {
			"PL2", "PL3L", "PL3U", "PL4",
			"PL2_R", "PL3L_R", "PL3U_R", "PL4_R",
			"PL2H1", "PL3LH1", "PL3UH1", "PL4H1",
			"PL2H1_R", "PL3LH1_R", "PL3UH1_R", "PL4H1_R",
			"PLOTONLY"
	};
	
	private static final String[] MODEL_DESCRIPTIONS = {
			"Two-parameter logistic model (OSB) with fixed upper and lower bound",
			"Three-parameter logistic model (OSB) with fixed lower bound",
			"Three-parameter logistic model (OSB) with fixed upper bound",
			"Four-parameter logistic model (OSB)",
			"Robust (weighted) variant of PL2",
			"Robust (weighted) variant of PL3L",
			"Robust (weighted) variant of PL3U",
			"Robust (weighted) variant of PL4",
			"Two-parameter logistic model (OSB) with fixed upper and lower bound, and hill fixed to 1",
			"Three-parameter logistic model (OSB) with fixed lower bound, and hill fixed to 1",
			"Three-parameter logistic model (OSB) with fixed upper bound, and hill fixed to 1",
			"Four-parameter logistic model (OSB), with hill fixed to 1",
			"Robust (weighted) variant of PL2H1",
			"Robust (weighted) variant of PL3LH1",
			"Robust (weighted) variant of PL3UH1",
			"Robust (weighted) variant of PL4H1",
			"Plots the data points but will not attempt any curve fitting"
	};
	
	@Override
	public String[] getSupportedModelIds() {
		return MODEL_IDS;
	}

	@Override
	public ICurveFitModel createModel(String id) {
		if (CollectionUtils.contains(MODEL_IDS, id)) {
			int index = IntStream.range(0, MODEL_IDS.length).filter(i -> MODEL_IDS[i].equals(id)).findAny().orElse(-1);
			return new OSBFitModel(id, MODEL_DESCRIPTIONS[index]);
		}
		else return null;
	}

}
