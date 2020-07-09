package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataViewerInput;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.DynamicColumns;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.EvaluationContext;


public class FormulaDataProvider<TEntity, TViewerElement> extends DataProvider<TEntity, TViewerElement> {
	
	
	private final long formulaId;
	
	private final EvaluationContext<TEntity> evaluationContext;
	
	
	public FormulaDataProvider(final DataDescription dataDescription,
			final AsyncDataViewerInput<TEntity, TViewerElement> viewerInput,
			final long formulaId,
			final EvaluationContext<TEntity> evaluationContext) {
		super(dataDescription, viewerInput);
		this.formulaId = formulaId;
		
		this.evaluationContext = evaluationContext;
	}
	
	
	@Override
	public Object apply(final TEntity element) {
		final Feature feature = this.evaluationContext.getFeature(element);
		if (feature == null) {
			return DynamicColumns.NO_FEATURE_SELECTED_STATUS;
		}
		final FormulaService formulaService = FormulaService.getInstance();
		final CalculationFormula formula = formulaService.getFormula(this.formulaId);
		if (formula == null) {
			return DynamicColumns.INVALID_CONFIG_STATUS;
		}
		final Object data = formulaService.evaluateFormula((Plate)element, feature, formula);
		return checkData(element, data);
	}
	
	
	@Override
	public int hashCode() {
		int hash = 41;
		hash = 31 * hash + (int)(this.formulaId ^ (this.formulaId >>> 32));
		return hash;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (super.equals(obj)) {
			final FormulaDataProvider other = (FormulaDataProvider)obj;
			return (this.formulaId == other.formulaId);
		}
		return false;
	}
	
}
