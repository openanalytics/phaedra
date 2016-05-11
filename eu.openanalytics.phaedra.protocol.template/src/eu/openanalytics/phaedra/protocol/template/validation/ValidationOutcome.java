package eu.openanalytics.phaedra.protocol.template.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.protocol.template.model.TemplateSettingKeys;

public class ValidationOutcome {

	private List<ValidationItem> validationItems;
	private Map<String, String> regexSettings;
	private TemplateSettingKeys keys;
	
	public ValidationOutcome() {
		validationItems = new ArrayList<>();
		regexSettings = new HashMap<>();
	}
	
	public List<ValidationItem> getValidationItems() {
		return validationItems;
	}
	
	public Map<String, String> getRegexSettings() {
		return regexSettings;
	}
	
	public TemplateSettingKeys getKeys() {
		return keys;
	}
	
	public void setKeys(TemplateSettingKeys keys) {
		this.keys = keys;
	}
}
