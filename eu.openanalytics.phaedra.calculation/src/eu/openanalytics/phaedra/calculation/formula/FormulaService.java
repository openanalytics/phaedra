package eu.openanalytics.phaedra.calculation.formula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.PermissionDeniedException;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.formula.language.JEPLanguage;
import eu.openanalytics.phaedra.calculation.formula.language.JavaScriptLanguage;
import eu.openanalytics.phaedra.calculation.formula.language.RLanguage;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRule;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRuleset;
import eu.openanalytics.phaedra.calculation.formula.model.InputType;
import eu.openanalytics.phaedra.calculation.formula.model.Language;
import eu.openanalytics.phaedra.calculation.formula.model.RulesetType;
import eu.openanalytics.phaedra.calculation.formula.model.Scope;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.property.ObjectPropertyService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class FormulaService extends BaseJPAService {

	private static final String CATEGORY_ALL = "All";
	private static final String DEFAULT_LANGUAGE_ID = JavaScriptLanguage.ID;
	
	private static FormulaService instance = new FormulaService();
	
	private Map<String, Language> languages;
	
	private FormulaService() {
		// Hidden constructor
		languages = new HashMap<>();
		languages.put(JavaScriptLanguage.ID, new JavaScriptLanguage());
		languages.put(JEPLanguage.ID, new JEPLanguage());
		languages.put(RLanguage.ID, new RLanguage());
	}
	
	public static FormulaService getInstance() {
		return instance;
	}
	
	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}
	
	public CalculationFormula getFormula(long id) {
		return getEntity(CalculationFormula.class, id);
	}
	
	public CalculationFormula getFormulaByName(String name) {
		return getEntity("select c from CalculationFormula c where c.name = ?1", CalculationFormula.class, name);
	}

	public List<CalculationFormula> getFormulae(String category) {
		List<CalculationFormula> formulae = null;
		if (category == null || category.equalsIgnoreCase(CATEGORY_ALL)) {
			formulae = streamableList(getList(CalculationFormula.class));
		} else {
			formulae = streamableList(getList("select c from CalculationFormula c where c.category = ?1", CalculationFormula.class, category));
		}
		formulae.sort((f1, f2) -> (int)(f1.getId() - f2.getId()));
		return formulae;
	}
	
	public String[] getFormulaNames() {
		return streamableList(getList("select c.name from CalculationFormula c", String.class)).stream().sorted().toArray(i -> new String[i]);
	}
	
	public String[] getFormulaCategories() {
		return getFormulaCategories(false);
	}
	
	public String[] getFormulaCategories(boolean includeCategoryAll) {
		List<String> categories = streamableList(getList("select c.category from CalculationFormula c where c.category is not null", String.class));
		if (includeCategoryAll) categories.add(CATEGORY_ALL);
		return categories.stream().sorted().toArray(i -> new String[i]);
	}
	
	public CalculationFormula createFormula() {
		CalculationFormula formula = new CalculationFormula();
		formula.setName("New formula");
		formula.setInputType(InputType.RawValue.getCode());
		formula.setScope(Scope.PerWell.getCode());
		formula.setLanguage(DEFAULT_LANGUAGE_ID);
		formula.setAuthor(SecurityService.getInstance().getCurrentUserName());
		formula.setFormula(generateExampleFormulaBody(formula));
		return formula;
	}
	
	public CalculationFormula getWorkingCopy(CalculationFormula formula) {
		CalculationFormula copy = new CalculationFormula();
		copy.setId(formula.getId());
		copyFormula(formula, copy);
		return copy;
	}
	
	public void copyFormula(CalculationFormula from, CalculationFormula to) {
		to.setName(from.getName());
		to.setDescription(from.getDescription());
		to.setCategory(from.getCategory());
		to.setFormula(from.getFormula());
		to.setInputType(from.getInputType());
		to.setScope(from.getScope());
		to.setLanguage(from.getLanguage());
	}
	
	public String generateExampleFormulaBody(CalculationFormula newFormula) {
		Language lang = getLanguage(newFormula.getLanguage());
		if (lang == null) return "";
		else return lang.generateExampleFormulaBody(newFormula);
	}
	
	public boolean canEditFormula(CalculationFormula formula) {
		String currentUser = SecurityService.getInstance().getCurrentUserName();
		String author = formula.getAuthor();
		return (author == null || author.equalsIgnoreCase(currentUser) || SecurityService.getInstance().isGlobalAdmin());
	}
	
	public void checkCanEditFormula(CalculationFormula formula) {
		if (!canEditFormula(formula)) throw new PermissionDeniedException(
				String.format("Only the author, %s, can modify the formula %s", formula.getAuthor(), formula.getName()));
	}
	
	public void updateFormula(CalculationFormula formula, CalculationFormula workingCopy) {
		if (workingCopy.getId() != formula.getId()) throw new IllegalArgumentException();
		checkCanEditFormula(formula);
		validateFormula(workingCopy);
		
		CalculationFormula conflict = getFormulaByName(workingCopy.getName());
		if (conflict != null && conflict.getId() != formula.getId()) {
			throw new IllegalArgumentException(String.format("A formula named %s already exists", workingCopy.getName()));
		}
		
		copyFormula(workingCopy, formula);
		save(formula);
	}
	
	public void deleteFormula(CalculationFormula formula) {
		checkCanEditFormula(formula);
		int count = getList("select r from FormulaRule r where r.formula.id = ?1", FormulaRule.class, formula.getId()).size();
		if (count == 0) {
			delete(formula);
		} else {
			throw new IllegalArgumentException(String.format("Cannot delete formula '%s': there are %d formula rules depending on it", formula.getName(), count));
		}
	}

	public void validateFormula(CalculationFormula formula) throws CalculationException {
		if (formula == null) throw new CalculationException("Invalid formula: null");
		if (formula.getName() == null || formula.getName().trim().isEmpty()) throw new CalculationException("Invalid formula: empty name");
		Language language = getLanguage(formula.getLanguage());
		if (language == null) throw new CalculationException("Invalid formula language: " + formula.getLanguage());
		language.validateFormula(formula);
		InputType type = FormulaUtils.getInputType(formula);
		if (type == null) throw new CalculationException("Invalid formula type: " + formula.getInputType());
		Scope scope = FormulaUtils.getScope(formula);
		if (scope == null) throw new CalculationException("Invalid formula scope: " + formula.getScope());
		if (formula.getFormula() == null || formula.getFormula().trim().isEmpty()) throw new CalculationException("Invalid formula: no formula body");
	}
	
	public Language[] getLanguages() {
		return languages.values().stream().sorted((l1, l2) -> l1.getLabel().compareTo(l2.getLabel())).toArray(i -> new Language[i]);
	}
	
	public Language getLanguage(String languageId) {
		if (languageId == null) return null;
		return languages.get(languageId);
	}
	
	public double[] evaluateFormula(Plate plate, Feature feature, CalculationFormula formula) throws CalculationException {
		return evaluateFormula(plate, feature, formula, null);
	}
	
	public double[] evaluateFormula(Plate plate, Feature feature, CalculationFormula formula, Map<String, Object> params) throws CalculationException {
		// Validate the formula
		FormulaService.getInstance().validateFormula(formula);
		
		// Assemble script input
		List<IValueObject> inputEntities = new ArrayList<>();
		switch (FormulaUtils.getScope(formula)) {
		case PerWell:
			inputEntities.addAll(plate.getWells());
			break;
		case PerPlate:
			inputEntities.add(plate);
			break;
		}

		double[] output = new double[plate.getWells().size()];
		Arrays.fill(output, Double.NaN);
		
		// Evaluate the formula
		long startTime = System.currentTimeMillis();
		Language language = FormulaService.getInstance().getLanguage(formula.getLanguage());
		inputEntities.parallelStream().forEach(inputValue -> {
			language.evaluateFormula(formula, inputValue, feature, output, params);
		});
		long duration = System.currentTimeMillis() - startTime;
		EclipseLog.debug(String.format("Formula %s evaluated on %s, feature %s in %d ms", formula.getName(), plate, feature, duration), CalculationService.class);
		
		return output;
	}
	
	/*
	 * Formula Rulesets
	 * ****************
	 */
	
	public FormulaRuleset getRuleset(long rulesetId) {
		return getEntity(FormulaRuleset.class, rulesetId);
	}
	
	public FormulaRuleset getRulesetForFeature(long featureId, int type) {
		return getEntity("select rs from FormulaRuleset rs where rs.feature.id = ?1 and rs.type = ?2", FormulaRuleset.class, featureId, type);
	}
	
	public Map<Long, FormulaRuleset> getRulesetsForProtocolClass(long protocolClassId, int type) {
		Map<Long, FormulaRuleset> rulesetsPerFeature = new HashMap<>();
		List<FormulaRuleset> rulesets = streamableList(getList("select rs from FormulaRuleset rs"
				+ " where rs.feature.protocolClass.id = ?1 and rs.type = ?2", FormulaRuleset.class, protocolClassId, type));
		for (FormulaRuleset rs: rulesets) rulesetsPerFeature.put(rs.getFeature().getId(), rs);
		return rulesetsPerFeature;
	}
	
	public FormulaRuleset createRuleset(Feature feature, int rulesetType) {
		if (feature == null) throw new IllegalArgumentException("Cannot create ruleset: null feature");
		FormulaRuleset ruleset = new FormulaRuleset();
		ruleset.setFeature(feature);
		ruleset.setType(RulesetType.HitCalling.getCode());
		ruleset.setRules(new ArrayList<>());
		checkCanEditRuleset(ruleset);
		return ruleset;
	}
	
	public FormulaRuleset getWorkingCopy(FormulaRuleset ruleset) {
		FormulaRuleset workingCopy = createRuleset(ruleset.getFeature(), ruleset.getType());
		copyRuleset(ruleset, workingCopy);
		return workingCopy;
	}
	
	public void copyRuleset(FormulaRuleset from, FormulaRuleset to) {
		to.setType(from.getType());
		to.setShowInUI(from.isShowInUI());
		to.setColor(from.getColor());
		to.setStyle(from.getStyle());
		to.setFeature(from.getFeature());
		to.setId(from.getId());
		
		List<FormulaRule> oldRules = new ArrayList<>(to.getRules());
		to.getRules().clear();
		for (FormulaRule newItem: from.getRules()) {
			FormulaRule itemToReplace = oldRules.stream().filter(i -> i.getId() == newItem.getId()).findAny().orElse(null);
			if (itemToReplace == null) itemToReplace = new FormulaRule();
			copyRule(newItem, itemToReplace);
			to.getRules().add(itemToReplace);
		}
	}
	
	private void copyRule(FormulaRule from, FormulaRule to) {
		to.setId(from.getId());
		to.setName(from.getName());
		to.setFormula(from.getFormula());
		to.setSequence(from.getSequence());
		to.setThreshold(from.getThreshold());
		to.setRuleset(from.getRuleset());
	}
	
	public boolean canEditRuleset(FormulaRuleset ruleset) {
		if (ruleset.getFeature() == null) return false;
		return ProtocolService.getInstance().canEditProtocolClass(ruleset.getFeature().getProtocolClass());
	}
	
	public void checkCanEditRuleset(FormulaRuleset ruleset) {
		if (!canEditRuleset(ruleset)) throw new PermissionDeniedException(
				String.format("No permission to modify the ruleset for feature %s", ruleset.getFeature()));
	}
	
	public void updateRuleset(FormulaRuleset ruleset, FormulaRuleset workingCopy) {
		if (workingCopy.getId() != ruleset.getId()) throw new IllegalArgumentException("Ruleset's working copy has a different ID");
		if (ruleset == workingCopy && ruleset.getId() != 0) throw new IllegalArgumentException("Cannot update a ruleset without a working copy");
		
		checkCanEditRuleset(ruleset);
		
		if (ruleset.getId() == 0 && getRulesetForFeature(ruleset.getFeature().getId(), ruleset.getType()) != null) {
			throw new IllegalArgumentException(String.format("Cannot create ruleset: a ruleset already exists"
					+ " for feature %s and type %s", ruleset.getFeature(), RulesetType.get(ruleset.getType()).getLabel()));
		}
		
		validateRuleset(workingCopy);
		
		if (ruleset != workingCopy) copyRuleset(workingCopy, ruleset);
		for (int i = 0; i < ruleset.getRules().size(); i++) {
			FormulaRule rule = ruleset.getRules().get(i);
			rule.setSequence(i);
		}
		save(ruleset);
	}
	
	public void deleteRuleset(FormulaRuleset ruleset) {
		checkCanEditRuleset(ruleset);
		delete(ruleset);
	}
	
	public FormulaRule createRule(FormulaRuleset ruleset) {
		checkCanEditRuleset(ruleset);
		FormulaRule rule = new FormulaRule();
		rule.setRuleset(ruleset);
		rule.setName("New rule");
		rule.setSequence(ruleset.getRules().size() + 1);
		ruleset.getRules().add(rule);
		return rule;
	}
	
	public void validateRuleset(FormulaRuleset ruleset) throws CalculationException {
		if (ruleset == null) throw new CalculationException("Invalid ruleset: null");
		if (ruleset.getFeature() == null) throw new CalculationException("Invalid ruleset: no feature specified");
		for (FormulaRule rule: ruleset.getRules()) validateRule(rule);
	}
	
	public void validateRule(FormulaRule rule) throws CalculationException {
		if (rule == null) throw new CalculationException("Invalid rule: null");
		if (rule.getName() == null || rule.getName().trim().isEmpty()) throw new CalculationException("Invalid rule: empty name");
		if (rule.getFormula() == null) throw new CalculationException("Invalid rule: no formula");
	}
	
	public double getCustomRuleThreshold(Plate plate, FormulaRule rule) {
		String propKey = "formula-th-rule#" + rule.getId();
		float customThreshold = ObjectPropertyService.getInstance().getNumericValue(Plate.class.getName(), plate.getId(), propKey);
		return customThreshold;
	}
	
	public void saveCustomRuleThresholds(List<Plate> plates, FormulaRule rule, double threshold) {
		for (Plate plate: plates) SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		
		long[] plateIds = plates.stream().mapToLong(p -> p.getId()).toArray();
		String propKey = "formula-th-rule#" + rule.getId();
		
		if (rule.getThreshold() == threshold) {
			ObjectPropertyService.getInstance().deleteValues(Plate.class.getName(), plateIds, propKey);
		} else {
			float[] thresholds = new float[plates.size()];
			Arrays.fill(thresholds, (float) threshold);
			ObjectPropertyService.getInstance().setValues(Plate.class.getName(), plateIds, propKey, thresholds);
		}
	}
}
