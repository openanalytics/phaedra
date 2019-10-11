package eu.openanalytics.phaedra.calculation.hitcall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	public HitCallRuleset getRulesetForFeature(long featureId) {
		return getEntity("select rs from HitCallRuleset rs where rs.feature.id = ?1", HitCallRuleset.class, featureId);
	}
	
	public Map<Long, HitCallRuleset> getRulesetsForProtocolClass(long protocolClassId) {
		Map<Long, HitCallRuleset> rulesetsPerFeature = new HashMap<>();
		List<HitCallRuleset> rulesets = streamableList(getList("select rs from HitCallRuleset rs where rs.feature.protocolClass.id = ?1", HitCallRuleset.class, protocolClassId));
		for (HitCallRuleset rs: rulesets) rulesetsPerFeature.put(rs.getFeature().getId(), rs);
		return rulesetsPerFeature;
	}
	
	public HitCallRuleset createRuleset(Feature feature) {
		if (feature == null) throw new IllegalArgumentException("Cannot create ruleset: null feature");
		HitCallRuleset ruleset = new HitCallRuleset();
		ruleset.setFeature(feature);
		ruleset.setRules(new ArrayList<>());
		checkCanEditRuleset(ruleset);
		return ruleset;
	}
	
	public HitCallRuleset getWorkingCopy(HitCallRuleset ruleset) {
		HitCallRuleset workingCopy = createRuleset(ruleset.getFeature());
		copyRuleset(ruleset, workingCopy);
		return workingCopy;
	}
	
	public void copyRuleset(HitCallRuleset from, HitCallRuleset to) {
		to.setShowInUI(from.isShowInUI());
		to.setColor(from.getColor());
		to.setStyle(from.getStyle());
		to.setFeature(from.getFeature());
		to.setId(from.getId());
		
		List<HitCallRule> oldRules = new ArrayList<>(to.getRules());
		to.getRules().clear();
		for (HitCallRule newItem: from.getRules()) {
			HitCallRule itemToReplace = oldRules.stream().filter(i -> i.getId() == newItem.getId()).findAny().orElse(null);
			if (itemToReplace == null) itemToReplace = new HitCallRule();
			copyRule(newItem, itemToReplace);
			to.getRules().add(itemToReplace);
		}
	}
	
	private void copyRule(HitCallRule from, HitCallRule to) {
		to.setId(from.getId());
		to.setName(from.getName());
		to.setFormula(from.getFormula());
		to.setSequence(from.getSequence());
		to.setThreshold(from.getThreshold());
		to.setRuleset(from.getRuleset());
	}
	
	public boolean canEditRuleset(HitCallRuleset ruleset) {
		if (ruleset.getFeature() == null) return false;
		return ProtocolService.getInstance().canEditProtocolClass(ruleset.getFeature().getProtocolClass());
	}
	
	public void checkCanEditRuleset(HitCallRuleset ruleset) {
		if (!canEditRuleset(ruleset)) throw new PermissionDeniedException(
				String.format("No permission to modify the ruleset for feature %s", ruleset.getFeature()));
	}
	
	public void updateRuleset(HitCallRuleset ruleset, HitCallRuleset workingCopy) {
		if (workingCopy.getId() != ruleset.getId()) throw new IllegalArgumentException("Ruleset's working copy has a different ID");
		if (ruleset == workingCopy && ruleset.getId() != 0) throw new IllegalArgumentException("Cannot update a ruleset without a working copy");
		
		checkCanEditRuleset(ruleset);
		
		if (ruleset.getId() == 0 && getRulesetForFeature(ruleset.getFeature().getId()) != null) throw new IllegalArgumentException(
				String.format("Cannot create ruleset: a ruleset already exists for feature %s", ruleset.getFeature()));
		
		validateRuleset(workingCopy);
		
		if (ruleset != workingCopy) copyRuleset(workingCopy, ruleset);
		for (int i = 0; i < ruleset.getRules().size(); i++) {
			HitCallRule rule = ruleset.getRules().get(i);
			rule.setSequence(i);
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
	
	public void validateRuleset(HitCallRuleset ruleset) throws CalculationException {
		if (ruleset == null) throw new CalculationException("Invalid ruleset: null");
		if (ruleset.getFeature() == null) throw new CalculationException("Invalid ruleset: no feature specified");
		for (HitCallRule rule: ruleset.getRules()) validateRule(rule);
	}
	
	public void validateRule(HitCallRule rule) throws CalculationException {
		if (rule == null) throw new CalculationException("Invalid rule: null");
		if (rule.getName() == null || rule.getName().trim().isEmpty()) throw new CalculationException("Invalid rule: empty name");
		if (rule.getFormula() == null) throw new CalculationException("Invalid rule: no formula");
	}
	
	//TODO Only CalculationService#calculate should be allowed to call this method
	public double[] runHitCalling(HitCallRuleset ruleset, Plate plate) throws CalculationException {
		SecurityService.getInstance().checkWithException(Permissions.PLATE_CALCULATE, plate);
		
		if (ruleset == null || ruleset.getRules() == null || ruleset.getRules().isEmpty()) throw new CalculationException("Cannot perform hit calling: no rules provided");
		if (plate == null) throw new CalculationException("Cannot perform hit calling: no plate provided");
		validateRuleset(ruleset);
		
		Feature feature = ruleset.getFeature();
		clearCache(plate, feature);
		CacheKey key = createCacheKey(plate, feature);
		double[] hitValues = null;
		
		for (HitCallRule rule: ruleset.getRules()) {
			if (rule.getFormula() == null) throw new CalculationException(String.format("Cannot perform hit calling: rule %s has no formula", rule.getName()));
			
			Map<String, Object> params = new HashMap<>();
			params.put("threshold", rule.getThreshold());
			
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
