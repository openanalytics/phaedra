package eu.openanalytics.phaedra.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.action.compound.InvalidateCompoundAction;
import eu.openanalytics.phaedra.validation.action.compound.ResetCompoundValidationAction;
import eu.openanalytics.phaedra.validation.action.compound.ValidateCompoundAction;
import eu.openanalytics.phaedra.validation.action.plate.ApprovePlateAction;
import eu.openanalytics.phaedra.validation.action.plate.DisapprovePlateAction;
import eu.openanalytics.phaedra.validation.action.plate.InvalidatePlateAction;
import eu.openanalytics.phaedra.validation.action.plate.ResetPlateApprovalAction;
import eu.openanalytics.phaedra.validation.action.plate.ResetPlateValidationAction;
import eu.openanalytics.phaedra.validation.action.plate.ValidatePlateAction;
import eu.openanalytics.phaedra.validation.action.well.AcceptWellAction;
import eu.openanalytics.phaedra.validation.action.well.RejectOutlierWellAction;
import eu.openanalytics.phaedra.validation.action.well.RejectWellAction;
import eu.openanalytics.phaedra.validation.action.well.ResetWellAction;
import eu.openanalytics.phaedra.validation.hook.ValidationHookManager;

/**
 * API for changing the status of various objects, such as:
 * <ul>
 * <li>Accepting and rejecting wells</li>
 * <li>Updating plate calculation status</li>
 * <li>Updating plate validation status</li>
 * <li>Updating plate approval status</li>
 * <li>Updating compound validation status</li>
 * <li>Updating compound approval status</li>
 * </ul>
 * <p>
 * Validation-related actions are always saved in a history table, and cannot be erased.
 * </p>
 */
public class ValidationService {

	private static ValidationService instance;

	private ValidationHookManager validationHookManager;

	private Map<Action,IValidationAction> actionHandlers;

	private ValidationService() {
		// Hidden constructor.
		validationHookManager = new ValidationHookManager();
		actionHandlers = new HashMap<ValidationService.Action, IValidationAction>();
		loadHandlers();
	}

	public static ValidationService getInstance() {
		if (instance == null) instance = new ValidationService();
		return instance;
	}

	/**
	 * Perform the given action on the given object(s).
	 * 
	 * @param action The {@link Action} to perform.
	 * @param remark An optional remark to include in the action.
	 * @param showError True to display an error message if the action fails. False to just throw the exception.
	 * @param objects The objects to perform the action on.
	 * @throws ValidationException If the action fails, for example because it is not allowed on the given objects.
	 */
	public void doAction(Action action, String remark, boolean showError, Object... objects) throws ValidationException {
		//TODO Return if nothing to do, e.g. accepting an accepted well.

		IValidationAction actionHandler = actionHandlers.get(action);
		if (actionHandler != null) {

			if (objects.length == 1 && objects[0] instanceof List) {
				List<?> list = (List<?>)objects[0];
				objects = list.toArray(new Object[list.size()]);
			}

			try {
				validationHookManager.preValidation(action, objects);
				actionHandler.run(remark, objects);
				validationHookManager.postValidation(action, objects);

				// Fire an event.
				ModelEvent event = new ModelEvent(objects, ModelEventType.ValidationChanged, 0);
				ModelEventService.getInstance().fireEvent(event);
			} catch (ValidationException e) {
				if (showError) {
					Throwable cause = (e.getCause() == null) ? e : e.getCause();
					IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, cause.getMessage(), cause.getCause());
					Runnable r = () -> ErrorDialog.openError(Display.getDefault().getActiveShell(), "Action Failed", e.getMessage(), status);
					if (Thread.currentThread() == Display.getDefault().getThread()) r.run();
					else Display.getDefault().syncExec(r);
				} else {
					throw e;
				}
			}
		}
	}

	/**
	 * Check if the given action is a well accept/reject/reset action.
	 * 
	 * @param action The action to check.
	 * @return True if the action is a well action.
	 */
	public static boolean isWellAction(Action action) {
		return (action == Action.REJECT_WELL || action == Action.REJECT_OUTLIER_WELL
				|| action == Action.ACCEPT_WELL || action == Action.RESET_WELL);
	}

	/**
	 * Check if the given action is a plate or compound validation/invalidation/reset action.
	 * 
	 * @param action The action to check.
	 * @return True if the action is a plate or compound action.
	 */
	public static boolean isValidationAction(Action action) {
		return (action == Action.VALIDATE_PLATE || action == Action.RESET_PLATE_VALIDATION || action == Action.INVALIDATE_PLATE
				|| action == Action.VALIDATE_COMPOUND || action == Action.RESET_COMPOUND_VALIDATION || action == Action.INVALIDATE_COMPOUND);
	}

	/**
	 * Check if the given action is a plate approval/disapproval/reset action.
	 * 
	 * @param action The action to check.
	 * @return True if the action is a plate action.
	 */
	public static boolean isApprovalAction(Action action) {
		return (action == Action.APPROVE_PLATE || action == Action.RESET_PLATE_APPROVAL || action == Action.DISAPPROVE_PLATE);
	}

	/**
	 * Retrieve the current validation and approval status of a plate, ignoring local caching.
	 * 
	 * @param plate The plate whose status should be retrieved.
	 * @return The current status of the plate.
	 */
	public PlateStatus getPlateStatus(Plate plate) {
		String sql = "select validate_status, approve_status from phaedra.hca_plate where plate_id = " + plate.getId();
		EntityManager em = Screening.getEnvironment().getEntityManager();
		Query query = em.createNativeQuery(sql);

		List<?> results = JDBCUtils.queryWithLock(query, em);
		if (!results.isEmpty()) {
			Object[] row = (Object[])results.get(0);
			PlateStatus status = new PlateStatus(((Number)row[0]).intValue(), ((Number)row[1]).intValue());
			return status;
		}

		return new PlateStatus(PlateValidationStatus.VALIDATION_NOT_SET.code, PlateApprovalStatus.APPROVAL_NOT_SET.code);
	}

	public static class PlateStatus {

		public PlateStatus(int validationStatusCode, int approvalStatusCode) {
			this.validationStatus = PlateValidationStatus.getByCode(validationStatusCode);
			this.approvalStatus = PlateApprovalStatus.getByCode(approvalStatusCode);
			this.validationSet = (validationStatus == PlateValidationStatus.VALIDATED || validationStatus == PlateValidationStatus.INVALIDATED);
			this.approvalSet = (approvalStatus == PlateApprovalStatus.APPROVED || approvalStatus == PlateApprovalStatus.DISAPPROVED);
		}

		public PlateValidationStatus validationStatus;
		public PlateApprovalStatus approvalStatus;
		public boolean validationSet;
		public boolean approvalSet;
	}

	public static enum Action {

		REJECT_WELL,
		REJECT_OUTLIER_WELL,
		ACCEPT_WELL,
		RESET_WELL,

		VALIDATE_PLATE,
		INVALIDATE_PLATE,
		RESET_PLATE_VALIDATION,

		VALIDATE_COMPOUND,
		INVALIDATE_COMPOUND,
		RESET_COMPOUND_VALIDATION,

		APPROVE_PLATE,
		DISAPPROVE_PLATE,
		RESET_PLATE_APPROVAL
	}

	public static enum WellStatus implements EntityStatus {

		ACCEPTED_DEFAULT(0, "Accepted (default)",
				"The data point is accepted, and will be included in all calculations and uploads"),
		ACCEPTED(1, "Accepted",
				"The data point is accepted, and will be included in all calculations and uploads"),
		REJECTED_PLATEPREP(-1, "Rejected (plate preparation)",
				"The data point will be excluded from all calculations and will not be visible in other systems"),
		REJECTED_DATACAPTURE(-2, "Rejected (data capture)",
				"The data point will be excluded from all calculations and will not be visible in other systems"),
		REJECTED_PHAEDRA(-4, "Rejected (biological outlier)",
				"The data point will be excluded from all calculations and will be marked as an outlier in other systems"),
		REJECTED_OUTLIER_PHAEDRA(-8, "Rejected (technical issue)",
				"The data point will be excluded from all calculations and will not be visible in other systems"),
		;

		private int code;
		private String label;
		private String description;

		WellStatus(int code, String label, String description) {
			this.code = code;
			this.label = label;
			this.description = description;
		}

		@Override
		public int getCode() {
			return code;
		}

		public String getLabel() {
			return label;
		}

		public String getDescription() {
			return description;
		}

		public static WellStatus getByCode(int code) {
			for (WellStatus s: WellStatus.values()) {
				if (s.getCode() == code) return s;
			}
			return null;
		}
	}

	public static enum PlateCalcStatus implements EntityStatus {

		CALCULATION_NEEDED(0),
		CALCULATION_OK(1),
		CALCULATION_NOT_POSSIBLE(-1),
		CALCULATION_ERROR(-2),
		;

		private int code;

		PlateCalcStatus(int code) {
			this.code = code;
		}

		@Override
		public int getCode() {
			return code;
		}

		public static PlateCalcStatus getByCode(int code) {
			for (PlateCalcStatus s: PlateCalcStatus.values()) {
				if (s.getCode() == code) return s;
			}
			return null;
		}
		
		public boolean matches(Plate plate) {
			return getCode() == plate.getCalculationStatus();
		}
	}

	public static enum PlateValidationStatus implements EntityStatus {

		VALIDATION_NOT_SET(0),
		VALIDATION_NOT_NEEDED(1),
		VALIDATED(2),
		INVALIDATED(-1),
		;

		private int code;

		PlateValidationStatus(int code) {
			this.code = code;
		}

		@Override
		public int getCode() {
			return code;
		}

		public boolean matches(Plate plate) {
			return getCode() == plate.getValidationStatus();
		}
		
		public static PlateValidationStatus getByCode(int code) {
			for (PlateValidationStatus s: PlateValidationStatus.values()) {
				if (s.getCode() == code) return s;
			}
			return null;
		}
	}

	public static enum PlateApprovalStatus implements EntityStatus {

		APPROVAL_NOT_SET(0),
		APPROVAL_NOT_NEEDED(1),
		APPROVED(2),
		DISAPPROVED(-1),
		;

		private int code;

		PlateApprovalStatus(int code) {
			this.code = code;
		}

		@Override
		public int getCode() {
			return code;
		}

		public static PlateApprovalStatus getByCode(int code) {
			for (PlateApprovalStatus s: PlateApprovalStatus.values()) {
				if (s.getCode() == code) return s;
			}
			return null;
		}
		
		public boolean matches(Plate plate) {
			return getCode() == plate.getApprovalStatus();
		}
	}

	public static enum PlateUploadStatus implements EntityStatus {

		UPLOAD_NOT_SET(0),
		UPLOAD_NOT_NEEDED(1),
		UPLOADED(2),
		;

		private int code;

		PlateUploadStatus(int code) {
			this.code = code;
		}

		@Override
		public int getCode() {
			return code;
		}

		public static PlateUploadStatus getByCode(int code) {
			for (PlateUploadStatus s: PlateUploadStatus.values()) {
				if (s.getCode() == code) return s;
			}
			return null;
		}
		
		public boolean matches(Plate plate) {
			return getCode() == plate.getUploadStatus();
		}
	}

	public static enum CompoundValidationStatus implements EntityStatus {

		VALIDATION_NOT_SET(0),
		VALIDATION_NOT_NEEDED(1),
		VALIDATED(2),
		INVALIDATED(-1),
		;

		private int code;

		CompoundValidationStatus(int code) {
			this.code = code;
		}

		@Override
		public int getCode() {
			return code;
		}

		public static CompoundValidationStatus getByCode(int code) {
			for (CompoundValidationStatus s: CompoundValidationStatus.values()) {
				if (s.getCode() == code) return s;
			}
			return null;
		}
		
		public boolean matches(Compound compound) {
			return getCode() == compound.getValidationStatus();
		}
	}

	public interface EntityStatus {
		int getCode();
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private void loadHandlers() {
		actionHandlers = new HashMap<Action, IValidationAction>();


		actionHandlers.put(Action.REJECT_WELL, new RejectWellAction());
		actionHandlers.put(Action.REJECT_OUTLIER_WELL, new RejectOutlierWellAction());
		actionHandlers.put(Action.ACCEPT_WELL, new AcceptWellAction());
		actionHandlers.put(Action.RESET_WELL, new ResetWellAction());

		actionHandlers.put(Action.VALIDATE_PLATE, new ValidatePlateAction());
		actionHandlers.put(Action.INVALIDATE_PLATE, new InvalidatePlateAction());
		actionHandlers.put(Action.RESET_PLATE_VALIDATION, new ResetPlateValidationAction());

		actionHandlers.put(Action.APPROVE_PLATE, new ApprovePlateAction());
		actionHandlers.put(Action.DISAPPROVE_PLATE, new DisapprovePlateAction());
		actionHandlers.put(Action.RESET_PLATE_APPROVAL, new ResetPlateApprovalAction());

		actionHandlers.put(Action.VALIDATE_COMPOUND, new ValidateCompoundAction());
		actionHandlers.put(Action.INVALIDATE_COMPOUND, new InvalidateCompoundAction());
		actionHandlers.put(Action.RESET_COMPOUND_VALIDATION, new ResetCompoundValidationAction());
	}
}
