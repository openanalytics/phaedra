package eu.openanalytics.phaedra.ui.plate.inspector.experiment;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class FeatureStatistics {

	private Experiment experiment;
	private Feature feature;

	private double[] zPrimes;
	
	// UR-015: Add Robust Z prime, Pearson and Spearman correlation coefficient to the plate statistics 
	private double[] robustZPrimes;
	private double[] pearsonCCs;
	private double[] pearsonPValues;
	private double[] spearmanCCs;
	private double[] spearmanPValues;
	
	private double[] sbs;
	private double[] sns;

	public FeatureStatistics() {
		this(null, null);
	}

	public FeatureStatistics(Experiment experiment, Feature feature) {
		this.experiment = experiment;
		this.feature = feature;
	}

	public void load() {
		if (experiment == null || feature == null
				|| !feature.getProtocolClass().equals(experiment.getProtocol().getProtocolClass())) {
			this.zPrimes = new double[0];
			this.sbs = new double[0];
			this.sns = new double[0];
		} else {
			List<Plate> plates = getPlates();

			int nrOfPlates = plates.size();
			zPrimes = new double[nrOfPlates];
			
			// UR-015: Initialize the new plate statistics 
			robustZPrimes = new double[nrOfPlates];
			pearsonCCs = new double[nrOfPlates];
			pearsonPValues = new double[nrOfPlates];
			spearmanCCs = new double[nrOfPlates];
			spearmanPValues = new double[nrOfPlates];
			
			sbs = new double[nrOfPlates];
			sns = new double[nrOfPlates];

			for (int i = 0; i < nrOfPlates; i++) {
				Plate p = plates.get(i);
				zPrimes[i] = StatService.getInstance().calculate("zprime", p, feature, null, null);
				
				// UR-015: Calculate Robust Z prime, Pearson and Spearman correlation coefficient plate statistics
				robustZPrimes[i] = StatService.getInstance().calculate("robustzprime", p, feature, null, null);
				pearsonCCs[i] = StatService.getInstance().calculate("pearsoncc", p, feature, null, null);
				pearsonPValues[i] = StatService.getInstance().calculate("pearsonpval", p, feature, null, null);
				spearmanCCs[i] = StatService.getInstance().calculate("spearmancc", p, feature, null, null);
				spearmanPValues[i] = StatService.getInstance().calculate("spearmanpval", p, feature, null, null);
				
				sns[i] = StatService.getInstance().calculate("sn", p, feature, null, null);
				sbs[i] = StatService.getInstance().calculate("sb", p, feature, null, null);
			}
		}
	}

	private List<Plate> getPlates() {
		if (experiment == null) return new ArrayList<>();
		return PlateService.getInstance().getPlates(experiment);
	}

	public double[] getZPrimes() {
		return zPrimes;
	}

	public double[] getSBS() {
		return sbs;
	}

	public double[] getSNS() {
		return sns;
	}

}
