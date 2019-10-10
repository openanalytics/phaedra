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
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.formula.language.JEPLanguage;
import eu.openanalytics.phaedra.calculation.formula.language.JavaScriptLanguage;
import eu.openanalytics.phaedra.calculation.formula.language.RLanguage;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.InputType;
import eu.openanalytics.phaedra.calculation.formula.model.Language;
import eu.openanalytics.phaedra.calculation.formula.model.Scope;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
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
		delete(formula);
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
}
