package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ScriptContext;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ScriptLanguage;


public class ExpressionScriptContext extends ScriptContext {
	// keep in sync with EditorScriptContext
	
	
	private final Map<String, Object> variables = new LinkedHashMap<String, Object>();
	
	
	public ExpressionScriptContext(final ScriptLanguage language) {
		super(language);
	}
	
	
	public void put(final String name, final String label,
			final String description, final Object value) {
		this.variables.put(name, value);
	}
	
	@Override
	public void putString(final String name, final String description, final String s) {
		this.variables.put(name, s);
	}
	
	@Override
	public void putValueObject(final String name, final String label,
			final String description, final IValueObject vo) {
		if (vo == null) {
			return;
		}
		if (getLanguage().supportDirectModelObject()) {
			this.variables.put(name, vo);
		}
		this.variables.put(name + "Id", vo.getId());
	}
	
	@Override
	public void putValueObject(final Class<?> type, final String description, final IValueObject vo) {
		final String name = getContextVariableName(type);
		putValueObject(name, null, description, vo);
	}
	
	@Override
	public void putValueObjects(final Class<?> type, final String description, final List<? extends IValueObject> vos) {
		if (vos == null) {
			return;
		}
		final String name = getContextVariableName(type);
		if (getLanguage().supportDirectModelObject()) {
			this.variables.put(name + "s", vos.toArray());
		}
		this.variables.put(name + "Ids", getIds(vos));
	}
	
	@Override
	public void putFunction(final String name, final String label, final String description,
			final Object function) {
		if (getLanguage().supportDirectModelObject()) {
			this.variables.put(name, function);
		}
	}
	
	
	private long[] getIds(final List<? extends IValueObject> vos) {
		final long[] ids = new long[vos.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = vos.get(i).getId();
		}
		return ids;
	}
	
	
	public Map<String, Object> getVariables() {
		return this.variables;
	}
	
}
