package eu.openanalytics.phaedra.protocol.template.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateException;
import eu.openanalytics.phaedra.protocol.template.internal.TemplateRepository;

public class TemplateSettingKeys {

	private Map<Pattern, TemplateSettingKey> keys;
	
	public TemplateSettingKeys(String templateId) throws ProtocolTemplateException {
		try (InputStream input = TemplateRepository.getSettingKeys(templateId)) {
			Gson gson = new Gson();
			Map<String, TemplateSettingKey> stringKeys = gson.fromJson(new InputStreamReader(input), new TypeToken<Map<String, TemplateSettingKey>>(){}.getType());
			keys = new HashMap<>();
			for (String stringKey: stringKeys.keySet()) {
				TemplateSettingKey value = stringKeys.get(stringKey);
				value.name = stringKey;
				if (value.type == null) value.type = "string";
				Pattern pattern = Pattern.compile(stringKey.replace(".", "\\.").replace("*", ".*"));
				keys.put(pattern, value);
			}
		} catch (IOException e) {
			throw new ProtocolTemplateException("Failed to load template setting keys", e);
		}
	}

	public int getKeyCount() {
		return keys.size();
	}
	
	public TemplateSettingKey[] getAll() {
		return keys.values().toArray(new TemplateSettingKey[keys.size()]);
	}
	
	public TemplateSettingKey get(String name) {
		for (Pattern p: keys.keySet()) {
			if (p.matcher(name).matches()) return keys.get(p);
		}
		return null;
	}
	
	public static class TemplateSettingKey {
		public String name;
		public String description;
		public String type;
		public String defaultValue;
	}
}
