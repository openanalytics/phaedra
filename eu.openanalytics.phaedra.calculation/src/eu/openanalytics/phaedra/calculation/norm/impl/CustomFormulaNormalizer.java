package eu.openanalytics.phaedra.calculation.norm.impl;

import javax.script.ScriptException;

import eu.openanalytics.phaedra.base.util.misc.FormulaDescriptor;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationLanguage;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.norm.INormalizer;
import eu.openanalytics.phaedra.calculation.norm.NormalizationException;
import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.calculation.norm.cache.NormalizedGrid;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CustomFormulaNormalizer implements INormalizer {
	
	
	@Override
	public String getId() {
		return NormalizationService.NORMALIZATION_CUSTOM;
	}

	@Override
	public String getDescription() {
		return "(value depends on the custom normalization formula)";
	}
	
	@Override
	public FormulaDescriptor getFormulaDescriptor() {
		return null;
	}
	
	@Override
	public NormalizedGrid calculate(NormalizationKey key) throws NormalizationException {
		
		if (!(key.getFeature() instanceof Feature)) throw new NormalizationException("Custom normalization is only supported for well features");
		
		Feature feature = (Feature)key.getFeature();
		Plate plate = (Plate)key.getDataToNormalize();
		
		int rows = plate.getRows();
		int cols = plate.getColumns();
		double[][] grid = new double[rows][cols];
		
		if (!feature.isNumeric()) return new NormalizedGrid(grid);
		
		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(plate);
		String formula = feature.getNormalizationFormula();
		String language = feature.getNormalizationLanguage();
		
		if (formula == null) {
			throw new NormalizationException("Cannot normalize: feature " + feature + " has no normalization formula");
		}
		
		// Calculate normalized values.
		for (Well well: plate.getWells()) {
			Double numericResult = null;
			try {
				String result = CalculationLanguage.get(language).eval(formula, accessor, well, feature);
				numericResult = Double.parseDouble(result);
			} catch (ScriptException | CalculationException e) {
				numericResult = Double.NaN;
			} catch (NumberFormatException e) {
				numericResult = Double.NaN;
			}
			
			grid[well.getRow()-1][well.getColumn()-1] = numericResult;
		}
		
		return new NormalizedGrid(grid);
	}

}
