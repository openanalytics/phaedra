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
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.ui.protocol.Activator;

public class EditFormulaHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		CalculationFormula formula = SelectionUtils.getSingleObject(selection, CalculationFormula.class, true);
		if (formula != null) execute(formula);
		return null;
	}
	
	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext) {
			Object selection = ((IEvaluationContext) evaluationContext).getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
			if (selection instanceof ISelection) {
				CalculationFormula formula = SelectionUtils.getSingleObject((ISelection) selection, CalculationFormula.class, true);
				setBaseEnabled(validateSelection(formula));
				return;
			}
		}
		setBaseEnabled(false);
	}
	
	private boolean validateSelection(CalculationFormula formula) {
		if (formula == null) return false;
		return FormulaService.getInstance().canEditFormula(formula);
	}
	
	public static boolean execute(CalculationFormula formula) {
		EditFormulaDialog dialog = new EditFormulaDialog(Display.getCurrent().getActiveShell(), formula) {
			@Override
			protected void okPressed() {
				try {
					FormulaService.getInstance().updateFormula(formula);
					super.okPressed();
				}
				catch (Exception e) {
					StatusManager.getManager().handle(new Status(
							IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to save the formula", e),
							StatusManager.SHOW | StatusManager.LOG | StatusManager.BLOCK);
				}
			}
		};
		return (dialog.open() == Dialog.OK);
	}
	
}