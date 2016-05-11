package eu.openanalytics.phaedra.ui.plate.inspector.experiment;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateUploadStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;

public class ExperimentStatistics {

	private Experiment experiment;
	private Feature feature;

	private int nrOfPlates;
	private int nrOfValidPlates;
	private int nrOfInvalidPlates;
	private int nrOfApprovedPlates;
	private int nrOfDisapprovedPlates;
	private int nrOfUploadedPlates;
	private int nrOfNotUploadedPlates;

	private double[] zPrimes;
	private double[] sbs;
	private double[] sns;

	public ExperimentStatistics() {
		this(null, null);
	}

	public ExperimentStatistics(Experiment experiment, Feature feature) {
		this.experiment = experiment;
		this.feature = feature;
	}

	private void loadExperimentStatistics() {
		List<Plate> plates = getPlates();

		nrOfValidPlates = 0;
		nrOfInvalidPlates = 0;
		nrOfApprovedPlates = 0;
		nrOfDisapprovedPlates = 0;
		nrOfUploadedPlates = 0;
		nrOfNotUploadedPlates = 0;
		nrOfPlates = plates.size();

		for (Plate p: plates) {
			if (p.getValidationStatus() == PlateValidationStatus.VALIDATED.getCode()) nrOfValidPlates++;
			if (p.getValidationStatus() == PlateValidationStatus.INVALIDATED.getCode()) nrOfInvalidPlates++;
			if (p.getApprovalStatus() == PlateApprovalStatus.APPROVED.getCode()) nrOfApprovedPlates++;
			if (p.getApprovalStatus() == PlateApprovalStatus.DISAPPROVED.getCode()) nrOfDisapprovedPlates++;

			if (p.getUploadStatus() == PlateUploadStatus.UPLOADED.getCode()) nrOfUploadedPlates++;
			if (p.getUploadStatus() == PlateUploadStatus.UPLOAD_NOT_NEEDED.getCode()
					|| (p.getApprovalStatus() < 0 && p.getUploadStatus() == 0)) nrOfNotUploadedPlates++;
		}
	}

	private void loadExperimentFeatureStatistics() {
		if (feature == null) {
			this.zPrimes = new double[0];
			this.sbs = new double[0];
			this.sns = new double[0];
		} else {
			List<Plate> plates = getPlates();

			int nrOfPlates = plates.size();
			zPrimes = new double[nrOfPlates];
			sbs = new double[nrOfPlates];
			sns = new double[nrOfPlates];

			for (int i = 0; i < nrOfPlates; i++) {
				Plate p = plates.get(i);
				zPrimes[i] = StatService.getInstance().calculate("zprime", p, feature, null, null);
				sns[i] = StatService.getInstance().calculate("sn", p, feature, null, null);
				sbs[i] = StatService.getInstance().calculate("sb", p, feature, null, null);
			}
		}
	}

	private List<Plate> getPlates() {
		if (experiment == null) return new ArrayList<>();
		return PlateService.getInstance().getPlates(experiment);
	}

	public void loadExperiment(Experiment experiment) {
		this.experiment = experiment;
		loadExperimentStatistics();
	}

	public void loadFeature(Feature feature) {
		this.feature = feature;
		loadExperimentFeatureStatistics();
	}

	public int getNrOfPlates() {
		return nrOfPlates;
	}

	public int getNrOfValidPlates() {
		return nrOfValidPlates;
	}

	public int getNrOfInvalidPlates() {
		return nrOfInvalidPlates;
	}

	public int getNrOfUnvalidatedPlates() {
		return nrOfPlates - nrOfValidPlates - nrOfInvalidPlates;
	}

	public int getNrOfApprovedPlates() {
		return nrOfApprovedPlates;
	}

	public int getNrOfDisapprovedPlates() {
		return nrOfDisapprovedPlates;
	}

	public int getNrOfUnapprovedPlates() {
		return nrOfPlates - nrOfApprovedPlates - nrOfDisapprovedPlates;
	}

	public int getNrOfUploadedPlates() {
		return nrOfUploadedPlates;
	}

	public int getNrOfNotUploadedPlates() {
		return nrOfNotUploadedPlates;
	}

	public int getNrOfUploadNotSetPlates() {
		return nrOfPlates - nrOfUploadedPlates - nrOfNotUploadedPlates;
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
