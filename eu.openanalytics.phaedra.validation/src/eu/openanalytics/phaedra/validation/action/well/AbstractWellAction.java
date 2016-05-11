package eu.openanalytics.phaedra.validation.action.well;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.model.log.ObjectLogService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.validation.IValidationAction;
import eu.openanalytics.phaedra.validation.ValidationException;
import eu.openanalytics.phaedra.validation.ValidationService;
import eu.openanalytics.phaedra.validation.ValidationService.PlateStatus;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;

public abstract class AbstractWellAction implements IValidationAction {

	@Override
	public void run(String remark, Object... objects) throws ValidationException {
		if (objects == null || objects.length == 0) return;
		
		// Order all affected wells by their plate(s).
		Map<Plate, List<Well>> affectedPlates = new HashMap<Plate, List<Well>>();
		for (Object object: objects) {
			Well well = (Well)object;
			List<Well> affectedWells = affectedPlates.get(well.getPlate());
			if (affectedWells == null) {
				affectedWells = new ArrayList<>();
				affectedPlates.put(well.getPlate(), affectedWells);
			}
			affectedWells.add(well);
		}
		
		// Check permission on all affected wells.
		for (Plate plate: affectedPlates.keySet()) {
			PlateStatus status = ValidationService.getInstance().getPlateStatus(plate);
			if (status.approvalSet) throw new ValidationException("Cannot change well status: plate is already (dis)approved.");
			
			for (Well well: affectedPlates.get(plate)) {
				if (well.getStatus() == WellStatus.REJECTED_PLATEPREP.getCode()) throw new ValidationException("Cannot change well status: well was rejected during plate preparation.");
				if (well.getStatus() == WellStatus.REJECTED_DATACAPTURE.getCode()) throw new ValidationException("Cannot change well status: well was rejected during data capture.");
				if (PlateUtils.isControl(well) && status.validationSet) throw new ValidationException("Cannot change control well status: plate is already (in)validated. This is only allowed on sample wells.");
				SecurityService.getInstance().checkWithException(Permissions.WELL_CHANGE_STATUS, well);
			}
		}
		
		int newStatus = getActionStatus().getCode();
		if (remark == null || remark.isEmpty()) remark = WellStatus.getByCode(newStatus).toString();
		
		// Perform the validation operation.
		for (Plate plate: affectedPlates.keySet()) {
			List<Well> wells = new ArrayList<>();
			List<String> oldStatuses = new ArrayList<>();
			for (Well well: affectedPlates.get(plate)) {
				int oldStatus = well.getStatus();
				well.setStatus(newStatus);
				wells.add(well);
				oldStatuses.add(""+oldStatus);
			}
			ObjectLogService.getInstance().batchLogChange(wells, "Status", oldStatuses, ""+newStatus, remark);
			PlateService.getInstance().updatePlateValidation(plate);
		}
	}
	
	protected abstract WellStatus getActionStatus();
}
