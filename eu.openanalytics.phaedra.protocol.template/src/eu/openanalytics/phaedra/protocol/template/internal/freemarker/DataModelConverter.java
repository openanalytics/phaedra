package eu.openanalytics.phaedra.protocol.template.internal.freemarker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class DataModelConverter {

	public static Object convert(Properties props) {
		Map<String, Object> dataModel = new HashMap<>();
		Set<Object> keys = props.keySet();
		// Sorting is important for handling lists (see setValue).
		List<String> keyList = keys.stream().map(k->(String)k).sorted().collect(Collectors.toList());
		for (Object key: keyList) {
			String[] keyParts = ((String)key).split("\\.");
			setValue(keyParts, props.getProperty((String)key), dataModel);
		}
		return dataModel;
	}
	
	@SuppressWarnings("unchecked")
	private static void setValue(String[] key, String value, Map<String, Object> dataModel) {
		if (key.length == 1) {
			dataModel.put(key[0], value);
		} else if (key.length > 2 && NumberUtils.isNumeric(key[1])) {
			// Build a List instead of a Map
			List<Map<String, Object>> list = (List<Map<String, Object>>) dataModel.get(key[0]);
			if (list == null) {
				list = new ArrayList<>();
				dataModel.put(key[0], list);
			}
			int index = Integer.parseInt(key[1]) - 1;
			Map<String, Object> subMap = null;
			if (index < list.size()) {
				subMap = list.get(index);
			} else {
				subMap = new HashMap<>();
				list.add(subMap);
			}
			String[] subKey = Arrays.copyOfRange(key, 2, key.length);
			setValue(subKey, value, subMap);
		} else {
			// Recursive map
			String[] subKey = Arrays.copyOfRange(key, 1, key.length);
			Map<String, Object> subMap = (Map<String, Object>) dataModel.get(key[0]);
			if (subMap == null) {
				subMap = new HashMap<>();
				dataModel.put(key[0], subMap);
			}
			setValue(subKey, value, subMap);
		}
	}
}
