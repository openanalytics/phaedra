package eu.openanalytics.phaedra.ui.protocol.calculation;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.hitcall.HitCallService;
import eu.openanalytics.phaedra.calculation.hitcall.model.HitCallRuleset;
import eu.openanalytics.phaedra.ui.protocol.Activator;

// Java.type("eu.openanalytics.phaedra.ui.protocol.calculation.EditHitCallRulesetHandler").execute(API.get("HitCallService").getRuleset(3))
public class EditHitCallRulesetHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		HitCallRuleset ruleset = SelectionUtils.getSingleObject(selection, HitCallRuleset.class, true);
		if (ruleset != null) execute(ruleset);
		return null;
	}
	
	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext) {
			Object selection = ((IEvaluationContext) evaluationContext).getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
			if (selection instanceof ISelection) {
				HitCallRuleset ruleset = SelectionUtils.getSingleObject((ISelection) selection, HitCallRuleset.class, true);
				setBaseEnabled(validateSelection(ruleset));
				return;
			}
		}
		setBaseEnabled(false);
	}
	
	private boolean validateSelection(HitCallRuleset ruleset) {
		if (ruleset == null) return false;
		return HitCallService.getInstance().canEditRuleset(ruleset);
	}
	
	public static boolean execute(HitCallRuleset ruleset) {
		HitCallRuleset workingCopy = HitCallService.getInstance().getWorkingCopy(ruleset);
		EditHitCallRulesetDialog dialog = new EditHitCallRulesetDialog(Display.getCurrent().getActiveShell(), workingCopy) {
			@Override
			protected void okPressed() {
				try {
					HitCallService.getInstance().updateRuleset(ruleset, workingCopy);
					super.okPressed();
				}
				catch (Exception e) {
					StatusManager.getManager().handle(new Status(
							IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to save the ruleset:", e),
							StatusManager.SHOW | StatusManager.LOG | StatusManager.BLOCK);
				}
			}
		};
		return (dialog.open() == Dialog.OK);
	}
	
}