package eu.openanalytics.phaedra.calculation.norm.impl;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationLanguage;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.jep.JEPCalculationService;
import eu.openanalytics.phaedra.calculation.norm.INormalizer;
import eu.openanalytics.phaedra.calculation.norm.NormalizationException;
import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.calculation.norm.cache.NormalizedGrid;
import eu.openanalytics.phaedra.calculation.stat.StatService;
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
		
		Map<String, Object> javaObjects = new HashMap<String, Object>();
		javaObjects.put("data", CalculationService.getInstance().getAccessor(plate));
		javaObjects.put("stats", StatService.getInstance());
		javaObjects.put("plate", plate);
		javaObjects.put("experiment", plate.getExperiment());
		javaObjects.put("feature", feature);
		
		// Calculate normalized values.
		for (Well well: plate.getWells()) {
			double rawValue = accessor.getNumericValue(well, feature, null);
			javaObjects.put("well", well);
			javaObjects.put("rawValue", rawValue);
			
			Double numericResult = null;
			try {
				CalculationLanguage lang = CalculationLanguage.getFor(language);
				String result = "";
				if (lang == CalculationLanguage.JEP) {
					result = "" + JEPCalculationService.getInstance().evaluate(formula, well);
				} else {
					Object retVal = ScriptService.getInstance().executeScript(formula, javaObjects, lang.getEngine());
					if (retVal != null) result = retVal.toString();
				}
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
