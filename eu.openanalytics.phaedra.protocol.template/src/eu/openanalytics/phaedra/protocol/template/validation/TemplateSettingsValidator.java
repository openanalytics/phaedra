package eu.openanalytics.phaedra.protocol.template.validation;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateException;
import eu.openanalytics.phaedra.protocol.template.internal.TemplateRepository;
import eu.openanalytics.phaedra.protocol.template.model.TemplateSettingKeys;
import eu.openanalytics.phaedra.protocol.template.model.TemplateSettingKeys.TemplateSettingKey;
import eu.openanalytics.phaedra.protocol.template.model.TemplateSettings;

public class TemplateSettingsValidator {

	public ValidationOutcome validate(String settings) {
		ValidationOutcome outcome = new ValidationOutcome();
		
		Properties props = new Properties();
		String templateId = null;
		
		try {
			props.load(new StringReader(settings.replace("\\", "\\\\")));
			templateId = props.getProperty(TemplateSettings.TEMPLATE_ID);
			if (templateId == null) {
				outcome.getValidationItems().add(new ValidationItem(0, settings.length()-1, ValidationItem.SEV_ERROR, "Missing required setting: " + TemplateSettings.TEMPLATE_ID));
			} else if (!TemplateRepository.templateExists(templateId)) {
				outcome.getValidationItems().add(new ValidationItem(0, settings.length()-1, ValidationItem.SEV_ERROR, "Unknown template: " + templateId));
			}
			outcome.setKeys(new TemplateSettingKeys(templateId));
		} catch (IOException | ProtocolTemplateException e) {
			outcome.getValidationItems().add(new ValidationItem(0, settings.length()-1, ValidationItem.SEV_WARNING, "Validation not available for template " + templateId));
		}

		// If problems were found up to here, they are critical problems which prevent further validation.
		if (outcome.getValidationItems().isEmpty()) {
			for (Object keyObject: props.keySet()) {
				String key = keyObject.toString();
				int start = settings.indexOf(key + "=");
				if (start == -1) start = settings.indexOf(key);
				int end = start + key.length();
				TemplateSettingKey keyDefinition = outcome.getKeys().get(key);
				if (keyDefinition == null) {
					outcome.getValidationItems().add(new ValidationItem(start, end, ValidationItem.SEV_WARNING, "Unknown setting: " + key));
				} else {
					String value = props.getProperty(key);
					if ("regex".equalsIgnoreCase(keyDefinition.type)) outcome.getRegexSettings().put(key, value);
					if (value == null || value.isEmpty()) {
						outcome.getValidationItems().add(new ValidationItem(start, end, ValidationItem.SEV_ERROR, "Value cannot be empty: " + key));
					}
				}
			}
		}
		
		return outcome;
	}
}
