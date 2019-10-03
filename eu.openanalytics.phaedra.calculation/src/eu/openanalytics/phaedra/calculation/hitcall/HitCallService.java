package eu.openanalytics.phaedra.calculation.hitcall;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.PermissionDeniedException;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.hitcall.model.HitCallRule;
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
	
	public CalculationFormula getFormula(long id) {
		return getEntity(CalculationFormula.class, id);
	}
	
	public CalculationFormula getFormulaByName(String name) {
		return getEntity("select c from CalculationFormula c where c.name = ?1", CalculationFormula.class, name);
	}

	public String[] getFormulaNames() {
		return streamableList(getList("select c.name from CalculationFormula c", String.class)).stream().sorted().toArray(i -> new String[i]);
	}
	
	public HitCallRule getRule(long id) {
		return getEntity(HitCallRule.class, id);
	}
	
	public HitCallRule createRule() {
		HitCallRule rule = new HitCallRule();
		rule.setName("New rule");
		return rule;
	}
	
	public boolean canEditRule(HitCallRule formula) {
		//TODO apply security to rules. Bind to protocol class?
		return (SecurityService.getInstance().isGlobalAdmin());
	}
	
	public void checkCanEditRule(HitCallRule rule) {
		if (!canEditRule(rule)) throw new PermissionDeniedException(String.format("No permission to modify the rule %s", rule.getName()));
	}
	
	public void updateRule(HitCallRule rule) {
		checkCanEditRule(rule);
		validateRule(rule);
		save(rule);
	}
	
	public void deleteRule(HitCallRule rule) {
		checkCanEditRule(rule);
		delete(rule);
	}

	public void validateRule(HitCallRule rule) throws CalculationException {
		if (rule == null) throw new CalculationException("Invalid rule: null");
		if (rule.getName() == null || rule.getName().trim().isEmpty()) throw new CalculationException("Invalid rule: empty name");
		if (rule.getFormula() == null) throw new CalculationException("Invalid rule: no formula");
	}
	
	
	public double[] runHitCalling(HitCallRule rule, Plate plate, Feature feature) throws CalculationException {
		return runHitCalling(Collections.singletonList(rule), plate, feature);
	}
	
	public double[] runHitCalling(List<HitCallRule> rules, Plate plate, Feature feature) throws CalculationException {
		if (rules == null || rules.isEmpty()) throw new CalculationException("Cannot perform hit calling: no rules provided");
		if (plate == null) throw new CalculationException("Cannot perform hit calling: no plate provided");
		if (feature == null) throw new CalculationException("Cannot perform hit calling: no feature provided");

		clearCache(plate, feature);
		CacheKey key = createCacheKey(plate, feature);
		double[] hitValues = null;
		
		for (HitCallRule rule: rules) {
			if (rule.getFormula() == null) throw new CalculationException(String.format("Cannot perform hit calling: rule %s has no formula", rule.getName()));
			double[] ruleHitValues = CalculationService.getInstance().evaluateFormula(plate, feature, rule.getFormula());
			
			boolean isFirstRule = (rule == rules.get(0));
			if (isFirstRule) hitValues = new double[ruleHitValues.length];
			
			for (int i = 0; i < ruleHitValues.length; i++) {
				if (ruleHitValues[i] >= rule.getThreshold()) {
					if (isFirstRule) hitValues[i] = 1.0;
					else hitValues[i] = Math.min(hitValues[i], 1.0);
				} else {
					hitValues[i] = 0;
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
		Well[] wells = streamableList(plate.getWells()).stream().sorted((w1, w2) -> (int)(w2.getId() - w1.getId())).toArray(i -> new Well[i]);
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
