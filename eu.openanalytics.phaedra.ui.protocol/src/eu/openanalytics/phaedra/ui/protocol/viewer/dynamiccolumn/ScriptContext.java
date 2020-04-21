package eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn;

import java.util.List;

import eu.openanalytics.phaedra.base.db.IValueObject;


public abstract class ScriptContext {
	
	
	private final ScriptLanguage language;
	
	
	public ScriptContext(final ScriptLanguage language) {
		this.language = language;
	}
	
	
	public ScriptLanguage getLanguage() {
		return this.language;
	}
	
	public boolean isEditorContext() {
		return false;
	}
	
	
	public abstract void putString(final String name, final String description, final String s);
	
	public abstract void putValueObject(final String name, final String label,
			final String description, final IValueObject vo);
	
	public void putValueObject(final Class<?> type, final String description, final IValueObject vo) {
		final String name = getContextVariableName(type);
		putValueObject(name, null, description, vo);
	}
	
	public abstract void putValueObjects(final Class<?> type, final String description, final List<? extends IValueObject> vos);
	
	public abstract void putFunction(final String name, final String label, final String description,
			final Object function);
	
	
	protected String getContextVariableName(final Class<?> type) {
		final String simpleName = type.getSimpleName();
		return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
	}
	
}
