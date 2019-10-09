package eu.openanalytics.phaedra.base.scripting.jep.parse;

import java.util.Map;

public class ContextVarScanner extends BaseScanner<Object> {

	@Override
	protected char getVarSign() {
		return '~';
	}

	@Override
	protected boolean isValidObject(Object obj) {
		// This scanner does not require any specific data object type.
		return true;
	}
	
	@Override
	protected Object getValueForRef(String scope, String[] fieldNames, Map<String, Object> context) {
		String varName = fieldNames[0];
		if (context == null) return null;
		else return context.get(varName);
	}
	
	@Override
	protected Object getValueForRef(String scope, String[] fieldNames, Object obj) {
		return null;
	}

}
