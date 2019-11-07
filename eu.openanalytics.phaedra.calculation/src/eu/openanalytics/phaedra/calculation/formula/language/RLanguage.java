package eu.openanalytics.phaedra.calculation.formula.language;

import java.util.Arrays;
import java.util.Map;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.r.rservi.RUtils;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.Scope;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class RLanguage extends BaseLanguage {

public static final String ID = "r";
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getLabel() {
		return "R";
	}

	@Override
	public String generateExampleFormulaBody(CalculationFormula formula) {
		return "inputValues * 100";
	}
	
	@Override
	public void validateFormula(CalculationFormula formula) throws CalculationException {
		super.validateFormula(formula);
		// Running an r node per well is very bad performance. It may also deadlock on the r session pool.
		if (formula.getScope() == Scope.PerWell.getCode()) throw new CalculationException("JEP cannot evaluate a formula per-well. Please select per-plate instead.");
	}

	@Override
		protected Map<String, Object> buildContext(CalculationFormula formula, IValueObject inputValue, IFeature feature) {
			Map<String, Object> context = super.buildContext(formula, inputValue, feature);
			
			Plate plate = (Plate) inputValue;
			Well[] wells = PlateService.streamableList(plate.getWells())
					.stream()
					.sorted(PlateUtils.WELL_NR_SORTER)
					.toArray(i -> new Well[i]);

			String[] colNames = { "WELL_NR", "WELLTYPE_CODE", "ROW_NR", "COL_NR", "INPUT_VALUES" };
			Object[] colValues = {
					Arrays.stream(wells).mapToInt(w -> PlateUtils.getWellNr(w)).toArray(),
					Arrays.stream(wells).map(Well::getWellType).toArray(i -> new String[i]),
					Arrays.stream(wells).mapToInt(Well::getRow).toArray(),
					Arrays.stream(wells).mapToInt(Well::getColumn).toArray(),
					context.get("inputValues")
			};
			context.put("inputDataFrame", RUtils.makeDataFrame(colNames, colValues));
			return context;
		}
	
	@Override
	public void transformFormulaOutput(IValueObject inputValue, Object outputValue, CalculationFormula formula, Map<String, Object> context, double[] outputArray) {
		for (int index=0; index<outputArray.length; index++) {
			if (outputValue instanceof double[]) outputArray[index] = ((double[]) outputValue)[index];
			else if (outputValue instanceof float[]) outputArray[index] = ((float[]) outputValue)[index];
			else if (outputValue instanceof long[]) outputArray[index] = ((long[]) outputValue)[index];
			else if (outputValue instanceof int[]) outputArray[index] = ((int[]) outputValue)[index];
		}		
	}
}
