package eu.openanalytics.phaedra.ui.protocol.calculation;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;

public class CreateFormulaHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		execute();
		return null;
	}
	
	public static CalculationFormula execute() {
		CalculationFormula formula = FormulaService.getInstance().createFormula();
		if (EditFormulaHandler.execute(formula)) return formula;
		return null;
	}
	
}