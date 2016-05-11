package eu.openanalytics.phaedra.validation.dialog;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationJobHelper;
import eu.openanalytics.phaedra.validation.ValidationService.Action;

public class InvalidatePlatesDialog extends BaseValidationDialog {

	private List<Plate> plates;
	
	public InvalidatePlatesDialog(Shell parentShell, List<Plate> plates) {
		super(parentShell);
		this.plates = plates;
	}

	@Override
	protected void fillDialogArea(Composite container) {
		setMessage("Provide a reason for the invalidation.");
		// Nothing to add beyond the reason text.
	}
	
	@Override
	protected String getTitle() {
		return "Invalidate Plate(s)";
	}

	@Override
	protected String getHistoryKey() {
		return "reason_history_invalid_plate";
	}

	@Override
	protected void doAction(String reason) {
		ValidationJobHelper.doInJob(Action.INVALIDATE_PLATE, reason, plates);
	}

}
