package eu.openanalytics.phaedra.validation.dialog;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationJobHelper;
import eu.openanalytics.phaedra.validation.ValidationService.Action;

public class DisapprovePlatesDialog extends BaseValidationDialog {

	private List<Plate> plates;
	
	public DisapprovePlatesDialog(Shell parentShell, List<Plate> plates) {
		super(parentShell);
		this.plates = plates;
	}

	@Override
	protected void fillDialogArea(Composite container) {
		setMessage("Provide a reason for the disapproval.");
		// Nothing to add beyond the reason text.
	}
	
	@Override
	protected String getTitle() {
		return "Disapprove Plate(s)";
	}
	
	@Override
	protected String getHistoryKey() {
		return "reason_history_disapprove_plate";
	}

	@Override
	protected void doAction(String reason) {
		ValidationJobHelper.doInJob(Action.DISAPPROVE_PLATE, reason, plates);
	}

}
