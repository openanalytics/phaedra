package eu.openanalytics.phaedra.calculation.norm.impl;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.FormulaDescriptor;
import eu.openanalytics.phaedra.calculation.Activator;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationLanguage;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.norm.INormalizer;
import eu.openanalytics.phaedra.calculation.norm.NormalizationException;
import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.calculation.norm.cache.NormalizedGrid;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CustomFormulaNormalizer implements INormalizer {
	
	private static final Pattern FORMULA_REF_PATTERN = Pattern.compile("formula#(\\d+)");
	
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
		
		if (formula == null) {
			throw new NormalizationException("Cannot normalize: feature " + feature + " has no normalization formula");
		}
		
		Matcher matcher = FORMULA_REF_PATTERN.matcher(formula);
		if (matcher.matches()) {
			long formulaId = Long.valueOf(matcher.group(1));
			CalculationFormula cFormula = FormulaService.getInstance().getFormula(formulaId);
			if (cFormula == null) throw new NormalizationException("Cannot normalize: formula with ID " + formulaId + " not found.");
			double[] normValues = new double[plate.getWells().size()];
			Arrays.fill(normValues, Double.NaN);
			try {
				normValues = FormulaService.getInstance().evaluateFormula(plate, feature, cFormula);
			} catch (CalculationException e) {
				EclipseLog.warn(String.format("Custom normalization failed: %s", cFormula), e, Activator.PLUGIN_ID);
			}
			for (Well well: plate.getWells()) {
				int wellNr = PlateUtils.getWellNr(well);
				grid[well.getRow()-1][well.getColumn()-1] = normValues[wellNr - 1];
			}
		} else {
			// Calculate normalized values.
			String language = feature.getNormalizationLanguage();
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
			
		}
		
		return new NormalizedGrid(grid);
	}
}