package eu.openanalytics.phaedra.calculation.hitcall;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
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

public class HitCallService extends BaseJPAService {

	private static HitCallService instance = new HitCallService();

	private ICache hitValueCache;
	private HitValueDAO hitValueDAO;
	
	private HitCallService() {
		// Hidden constructor
		hitValueCache = CacheService.getInstance().createCache("HitCallCache");
		hitValueDAO = new HitValueDAO();
	}

	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}
	
	public static HitCallService getInstance() {
		return instance;
	}
	
	
	// Note: only CalculationService#calculate should be allowed to call this method
	public double[] runHitCalling(FormulaRuleset ruleset, Plate plate) throws CalculationException {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_CALCULATE, plate);
		
		if (ruleset == null || ruleset.getRules() == null || ruleset.getRules().isEmpty()) throw new CalculationException("Cannot perform hit calling: no rules provided");
		if (ruleset.getType() != RulesetType.HitCalling.getCode()) throw new CalculationException("Cannot perform hit calling: provided ruleset is not of type " + RulesetType.HitCalling.getLabel());
		if (plate == null) throw new CalculationException("Cannot perform hit calling: no plate provided");
		FormulaService.getInstance().validateRuleset(ruleset);
		
		Feature feature = ruleset.getFeature();
		clearCache(plate, feature);
		CacheKey key = createCacheKey(plate, feature);
		double[] hitValues = null;
		
		for (FormulaRule rule: ruleset.getRules()) {
			if (rule.getFormula() == null) throw new CalculationException(String.format("Cannot perform hit calling: rule %s has no formula", rule.getName()));
			
			Map<String, Object> params = new HashMap<>();
			double threshold = FormulaService.getInstance().getCustomRuleThreshold(plate, rule);
			if (Double.isNaN(threshold)) threshold = rule.getThreshold();
			params.put("threshold", threshold);
			
			double[] ruleHitValues = FormulaService.getInstance().evaluateFormula(plate, feature, rule.getFormula(), params);
			
			boolean isFirstRule = (rule == ruleset.getRules().get(0));
			if (isFirstRule) hitValues = new double[ruleHitValues.length];
			
			for (int i = 0; i < ruleHitValues.length; i++) {
				if (Double.isNaN(ruleHitValues[i])) {
					hitValues[i] = 0;
				} else if (isFirstRule) {
					hitValues[i] = ruleHitValues[i];;
				} else {
					hitValues[i] = Math.min(hitValues[i], ruleHitValues[i]);
				}
			}
		}
		
		long[] wellIds = streamableList(plate.getWells()).stream().mapToLong(w -> w.getId()).toArray();
		hitValueDAO.saveHitValues(wellIds, feature.getId(), hitValues);
		hitValueCache.put(key, hitValues);
		
		return hitValues;
	}

	public double[] getHitValues(Plate plate, Feature feature) {
		CacheKey key = createCacheKey(plate, feature);
		if (hitValueCache.contains(key)) return (double[]) hitValueCache.get(key);
		
		double[] hitValues = null;
		synchronized (plate) {
			if (hitValueCache.contains(key)) return (double[]) hitValueCache.get(key);
			long[] wellIds = streamableList(plate.getWells()).stream().mapToLong(w -> w.getId()).toArray();
			hitValues = hitValueDAO.getHitValues(wellIds, feature.getId());
			hitValues = sortValues(hitValues, plate);
			hitValueCache.put(key, hitValues);
		}
		
		return hitValues;
	}
	
	public void clearCache(Plate plate, Feature feature) {
		if (plate == null && feature == null) return;
		hitValueCache.remove(createCacheKey(plate, feature), true);
	}
	
	private double[] sortValues(double[] hitValues, Plate plate) {
		// hitValues are sorted by wellId. Return an array sorted by wellNr instead.
		Well[] wells = streamableList(plate.getWells()).stream().sorted((w1, w2) -> (int)(w1.getId() - w2.getId())).toArray(i -> new Well[i]);
		double[] sortedValues = new double[hitValues.length];
		for (int i = 0; i < sortedValues.length; i++) {
			int wellNr = PlateUtils.getWellNr(wells[i]);
			sortedValues[wellNr - 1] = hitValues[i];
		}
		return sortedValues;
	}
	
	private CacheKey createCacheKey(Plate plate, Feature feature) {
		return CacheKey.create(plate.getId(), feature.getId());
	}
}
