package eu.openanalytics.phaedra.validation;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateCalcStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateUploadStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;

public class ValidationUtils {

	public static void checkCanModifyPlate(Plate plate) throws ValidationException {
		//TODO This doesn't take multiplo or exp-norm into account.
		// It is used by subwell data modification. Should be deprecated and replaced by pre-change hooks.
		PlateStatus plateStatus = ValidationService.getInstance().getPlateStatus(plate);
		if (plateStatus != null && plateStatus.validationStatus == PlateValidationStatus.VALIDATED) throw new ValidationException("Cannot modify plate: plate is validated.");
	}
	
	public static void applyRejectReason(List<Well> wells, String reason) {
		if (wells == null || reason == null || reason.isEmpty()) return;
		for (Well well : wells) {
			String description = well.getDescription();

			if (description == null || description.isEmpty()) {
				// No description yet, add
				description = "Rejected: " + reason + ";";
			} else {
				// Description present, check for Rejected reason
				int start = description.indexOf("Rejected: ");
				if (start > -1) {
					// Rejection reason present, replace with new one
					int end = description.indexOf(";", start);
					if (end < 0) end = description.length() - 1;
					description = description.replace(description.subSequence(start, end + 1), "Rejected: " + reason + ";");
				} else {
					// Rejection reason not present, append
					description += "; Rejected: " + reason + ";";
				}
			}
			// Trim to max size
			well.setDescription(StringUtils.trim(description, 200));
		}
	}
	
	public static void clearRejectReason(List<Well> wells) {
		if (wells == null) return;
		for (Well well: wells) {
			String desc = well.getDescription();
			if (desc != null && !desc.isEmpty()) {
				String pattern = "(.*)Rejected: .*;(.*)";
				Matcher matcher = Pattern.compile(pattern).matcher(desc);
				if (matcher.matches()) {
					String pre = matcher.group(1) == null ? "" : matcher.group(1);
					String post = matcher.group(2) == null ? "" : matcher.group(2);
					if (pre.isEmpty() && post.isEmpty()) desc = "";
					else desc = pre + " " + post;
					well.setDescription(desc);
				}
			}
		}
	}
	
	public static String getIcon(PlateCalcStatus status) {
		int code = status.getCode();
		if (code < 0) return "flag_red.png";
		if (code == 0) return "flag_white.png";
		return "flag_green.png";
	}
	
	public static String getIcon(PlateValidationStatus status) {
		return getDefaultIcons(status.getCode());
	}
	
	public static String getIcon(PlateApprovalStatus status) {
		return getDefaultIcons(status.getCode());
	}
	
	public static String getIcon(PlateUploadStatus status) {
		return getDefaultIcons(status.getCode());
	}
	
	private static String getDefaultIcons(int code) {
		if (code < 0) return "flag_red.png";
		if (code == 1) return "flag_blue.png";
		if (code > 1) return "flag_green.png";
		return "flag_white.png";
	}
}
