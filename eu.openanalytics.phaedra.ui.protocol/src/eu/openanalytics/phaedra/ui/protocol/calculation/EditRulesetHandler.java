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
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRuleset;
import eu.openanalytics.phaedra.ui.protocol.Activator;

public class EditRulesetHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		FormulaRuleset ruleset = SelectionUtils.getSingleObject(selection, FormulaRuleset.class, true);
		if (ruleset != null) execute(ruleset);
		return null;
	}
	
	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext) {
			Object selection = ((IEvaluationContext) evaluationContext).getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
			if (selection instanceof ISelection) {
				FormulaRuleset ruleset = SelectionUtils.getSingleObject((ISelection) selection, FormulaRuleset.class, true);
				setBaseEnabled(validateSelection(ruleset));
				return;
			}
		}
		setBaseEnabled(false);
	}
	
	private boolean validateSelection(FormulaRuleset ruleset) {
		if (ruleset == null) return false;
		return FormulaService.getInstance().canEditRuleset(ruleset);
	}
	
	public static boolean execute(FormulaRuleset ruleset) {
		FormulaRuleset workingCopy = FormulaService.getInstance().getWorkingCopy(ruleset);
		EditRulesetDialog dialog = new EditRulesetDialog(Display.getCurrent().getActiveShell(), workingCopy) {
			@Override
			protected void okPressed() {
				try {
					FormulaService.getInstance().updateRuleset(ruleset, workingCopy);
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