package eu.openanalytics.phaedra.ui.plate.inspector.experiment;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateUploadStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;

public class ExperimentStatistics {

	private Experiment experiment;

	private int nrOfPlates;
	private int nrOfValidPlates;
	private int nrOfInvalidPlates;
	private int nrOfApprovedPlates;
	private int nrOfDisapprovedPlates;
	private int nrOfUploadedPlates;
	private int nrOfNotUploadedPlates;

	public ExperimentStatistics() {
		this(null);
	}

	public ExperimentStatistics(Experiment experiment) {
		this.experiment = experiment;
	}

	public void load() {
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

	private List<Plate> getPlates() {
		if (experiment == null) return new ArrayList<>();
		return PlateService.getInstance().getPlates(experiment);
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

}
