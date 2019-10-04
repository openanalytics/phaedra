package eu.openanalytics.phaedra.calculation.hitcall;

import java.util.ArrayList;

import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.PermissionDeniedException;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.hitcall.model.HitCallRule;
import eu.openanalytics.phaedra.calculation.hitcall.model.HitCallRuleset;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

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
	
	public HitCallRuleset getRuleset(long rulesetId) {
		return getEntity(HitCallRuleset.class, rulesetId);
	}
	
	public HitCallRuleset getRulesetForProtocolClass(long protocolClassId) {
		return getEntity("select rs from HitCallRuleset rs where rs.protocolClass.id = ?1", HitCallRuleset.class, protocolClassId);
	}
	
	public HitCallRuleset createRuleset(ProtocolClass protocolClass) {
		if (protocolClass == null) throw new IllegalArgumentException("Cannot create ruleset: null protocolclass");
		if (getRulesetForProtocolClass(protocolClass.getId()) != null) throw new IllegalArgumentException(
				String.format("Cannot create ruleset: a ruleset already exists for protocolclass %s", protocolClass));
		HitCallRuleset ruleset = new HitCallRuleset();
		ruleset.setProtocolClass(protocolClass);
		ruleset.setRules(new ArrayList<>());
		checkCanEditRuleset(ruleset);
		return ruleset;
	}
	
	public boolean canEditRuleset(HitCallRuleset ruleset) {
		if (ruleset.getProtocolClass() == null) return false;
		return ProtocolService.getInstance().canEditProtocolClass(ruleset.getProtocolClass());
	}
	
	public void checkCanEditRuleset(HitCallRuleset ruleset) {
		if (!canEditRuleset(ruleset)) throw new PermissionDeniedException(
				String.format("No permission to modify the ruleset for protocolclass %s", ruleset.getProtocolClass()));
	}
	
	public void updateRuleset(HitCallRuleset ruleset) {
		checkCanEditRuleset(ruleset);
		for (int i = 0; i < ruleset.getRules().size(); i++) {
			HitCallRule rule = ruleset.getRules().get(i);
			validateRule(rule);
			rule.setSequence(i+1);
		}
		save(ruleset);
	}
	
	public void deleteRuleset(HitCallRuleset ruleset) {
		checkCanEditRuleset(ruleset);
		delete(ruleset);
	}
	
	public HitCallRule createRule(HitCallRuleset ruleset) {
		checkCanEditRuleset(ruleset);
		HitCallRule rule = new HitCallRule();
		rule.setRuleset(ruleset);
		rule.setName("New rule");
		rule.setSequence(ruleset.getRules().size() + 1);
		ruleset.getRules().add(rule);
		return rule;
	}
	
	public void validateRule(HitCallRule rule) throws CalculationException {
		if (rule == null) throw new CalculationException("Invalid rule: null");
		if (rule.getName() == null || rule.getName().trim().isEmpty()) throw new CalculationException("Invalid rule: empty name");
		if (rule.getFormula() == null) throw new CalculationException("Invalid rule: no formula");
	}
	
	//TODO Only CalculationService#calculate should be allowed to call this method
	public double[] runHitCalling(HitCallRuleset ruleset, Plate plate, Feature feature) throws CalculationException {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_CALCULATE, plate);
		
		if (ruleset == null || ruleset.getRules() == null || ruleset.getRules().isEmpty()) throw new CalculationException("Cannot perform hit calling: no rules provided");
		if (plate == null) throw new CalculationException("Cannot perform hit calling: no plate provided");
		if (feature == null) throw new CalculationException("Cannot perform hit calling: no feature provided");
		
		clearCache(plate, feature);
		CacheKey key = createCacheKey(plate, feature);
		double[] hitValues = null;
		
		for (HitCallRule rule: ruleset.getRules()) {
			if (rule.getFormula() == null) throw new CalculationException(String.format("Cannot perform hit calling: rule %s has no formula", rule.getName()));
			double[] ruleHitValues = FormulaService.getInstance().evaluateFormula(plate, feature, rule.getFormula());
			
			boolean isFirstRule = (rule == ruleset.getRules().get(0));
			if (isFirstRule) hitValues = new double[ruleHitValues.length];
			
			// If any rule evaluates to 0, the outcome is 0
			// Otherwise, the outcome is 1
			for (int i = 0; i < ruleHitValues.length; i++) {
				if (!Double.isNaN(ruleHitValues[i]) && ruleHitValues[i] >= rule.getThreshold()) {
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
