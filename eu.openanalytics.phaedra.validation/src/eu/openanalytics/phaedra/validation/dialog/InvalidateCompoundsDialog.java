package eu.openanalytics.phaedra.validation.dialog;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.validation.ValidationJobHelper;
import eu.openanalytics.phaedra.validation.ValidationService.Action;

public class InvalidateCompoundsDialog extends BaseValidationDialog {

	private List<Compound> compounds;
	
	public InvalidateCompoundsDialog(Shell parentShell, List<Compound> compounds) {
		super(parentShell);
		this.compounds = compounds;
	}

	@Override
	protected void fillDialogArea(Composite container) {
		setMessage("Provide a reason for the invalidation.");
		// Nothing to add beyond the reason text.
	}
	
	@Override
	protected String getTitle() {
		return "Invalidate Compound(s)";
	}

	@Override
	protected String getHistoryKey() {
		return "reason_history_invalid_compound";
	}

	@Override
	protected void doAction(String reason) {
		ValidationJobHelper.doInJob(Action.INVALIDATE_COMPOUND, reason, compounds);
	}

}
