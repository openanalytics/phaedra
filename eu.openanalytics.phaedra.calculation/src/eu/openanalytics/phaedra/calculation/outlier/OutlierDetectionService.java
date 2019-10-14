package eu.openanalytics.phaedra.calculation.outlier;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRule;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRuleset;
import eu.openanalytics.phaedra.calculation.formula.model.RulesetType;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class OutlierDetectionService {

	private static OutlierDetectionService instance = new OutlierDetectionService();
	
	private ICache outlierValueCache;
	private OutlierValueDAO outlierValueDAO;
	
	private OutlierDetectionService() {
		// Hidden constructor
		outlierValueCache = CacheService.getInstance().createCache("OutlierValueCache");
		outlierValueDAO = new OutlierValueDAO();
	}
	
	public static OutlierDetectionService getInstance() {
		return instance;
	}

	// Note: only CalculationService#calculate should be allowed to call this method
	public void runOutlierDetection(FormulaRuleset ruleset, Plate plate) throws CalculationException {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_CALCULATE, plate);
		
		if (ruleset == null || ruleset.getRules() == null || ruleset.getRules().isEmpty()) throw new CalculationException("Cannot perform outlier detection: no rules provided");
		if (ruleset.getType() != RulesetType.OutlierDetection.getCode()) throw new CalculationException("Cannot perform outlier detection: provided ruleset is not of type " + RulesetType.OutlierDetection.getLabel());
		if (plate == null) throw new CalculationException("Cannot perform outlier detection: no plate provided");
		FormulaService.getInstance().validateRuleset(ruleset);
		
		Feature feature = ruleset.getFeature();
		clearCache(plate, feature);
		CacheKey key = createCacheKey(plate, feature);
		double[] outlierValues = null;
		
		for (FormulaRule rule: ruleset.getRules()) {
			if (rule.getFormula() == null) throw new CalculationException(String.format("Cannot perform outlier detection: rule %s has no formula", rule.getName()));
			
			Map<String, Object> params = new HashMap<>();
			double threshold = FormulaService.getInstance().getCustomRuleThreshold(plate, rule);
			if (Double.isNaN(threshold)) threshold = rule.getThreshold();
			params.put("threshold", threshold);
			
			double[] ruleOutlierValues = FormulaService.getInstance().evaluateFormula(plate, feature, rule.getFormula(), params);
			
			boolean isFirstRule = (rule == ruleset.getRules().get(0));
			if (isFirstRule) outlierValues = new double[ruleOutlierValues.length];
			
			for (int i = 0; i < ruleOutlierValues.length; i++) {
				if (Double.isNaN(ruleOutlierValues[i])) {
					outlierValues[i] = 0;
				} else if (isFirstRule) {
					outlierValues[i] = ruleOutlierValues[i];;
				} else {
					outlierValues[i] = Math.max(outlierValues[i], ruleOutlierValues[i]);
				}
			}
		}
		
		long[] wellIds = FormulaService.streamableList(plate.getWells()).stream().mapToLong(w -> w.getId()).toArray();
		outlierValueDAO.saveOutlierValues(wellIds, feature.getId(), outlierValues);
		outlierValueCache.put(key, toBoolean(outlierValues));
	}

	public boolean[] getOutliers(Plate plate, Feature feature) {
		CacheKey key = createCacheKey(plate, feature);
		if (outlierValueCache.contains(key)) return (boolean[]) outlierValueCache.get(key);
		
		boolean[] outlierValues = null;
		synchronized (plate) {
			if (outlierValueCache.contains(key)) return (boolean[]) outlierValueCache.get(key);
			long[] wellIds = FormulaService.streamableList(plate.getWells()).stream().mapToLong(w -> w.getId()).toArray();
			double[] v = sortValues(outlierValueDAO.getOutlierValues(wellIds, feature.getId()), plate);
			outlierValues = toBoolean(v);
			outlierValueCache.put(key, outlierValues);
		}
		
		return outlierValues;
	}
	
	public void clearCache(Plate plate, Feature feature) {
		if (plate == null && feature == null) return;
		outlierValueCache.remove(createCacheKey(plate, feature), true);
	}
	
	private boolean[] toBoolean(double[] outlierValues) {
		boolean[] booleanValues = new boolean[outlierValues.length];
		for (int i = 0; i < booleanValues.length; i++) {
			booleanValues[i] = !Double.isNaN(outlierValues[i]) && outlierValues[i] > 0.0d;
		}
		return booleanValues;
	}
	
	private double[] sortValues(double[] outlierValues, Plate plate) {
		// values are sorted by wellId. Return an array sorted by wellNr instead.
		Well[] wells = FormulaService.streamableList(plate.getWells()).stream().sorted((w1, w2) -> (int)(w1.getId() - w2.getId())).toArray(i -> new Well[i]);
		double[] sortedValues = new double[outlierValues.length];
		for (int i = 0; i < sortedValues.length; i++) {
			int wellNr = PlateUtils.getWellNr(wells[i]);
			sortedValues[wellNr - 1] = outlierValues[i];
		}
		return sortedValues;
	}
	
	private CacheKey createCacheKey(Plate plate, Feature feature) {
		return CacheKey.create(plate.getId(), feature.getId());
	}
}
