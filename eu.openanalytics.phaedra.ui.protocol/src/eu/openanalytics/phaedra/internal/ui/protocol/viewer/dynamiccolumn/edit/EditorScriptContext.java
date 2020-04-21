package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.edit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ScriptContext;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ScriptLanguage;


public class EditorScriptContext extends ScriptContext {
	
	
	private final Map<String, IContentProposal> variables = new LinkedHashMap<String, IContentProposal>();
	
	
	public EditorScriptContext(final ScriptLanguage language) {
		super(language);
	}
	
	
	@Override
	public boolean isEditorContext() {
		return true;
	}
	
	
	private void add(final String name, final String label, final String description) {
		this.variables.put(name, new ContentProposal(name, label, description));
	}
	
	public void put(final String name, final String label,
			final String description, final Object value) {
		add(name, label, description);
	}
	
	@Override
	public void putString(final String name, final String description, final String s) {
		add(name, name + " : String", description);
	}
	
	@Override
	public void putValueObject(final String name, final String label,
			final String description, final IValueObject vo) {
		if (getLanguage().supportDirectModelObject()) {
			add(name, label, description);
		}
		add(name + "Id", name + "Id : long", (description != null) ? "the id of " + description : null);
	}
	
	@Override
	public void putValueObject(Class<?> type, String description, IValueObject vo) {
		final String name = getContextVariableName(type);
		putValueObject(name, name + " : " + type.getSimpleName(), description, vo);
	}
	
	@Override
	public void putValueObjects(final Class<?> type, final String description, final List<? extends IValueObject> vos) {
		final String name = getContextVariableName(type);
		if (getLanguage().supportDirectModelObject()) {
			add(name + "s", name + "s : " + type.getSimpleName() + "[]", description);
		}
		add(name + "Ids", name + "Ids : long[]", (description != null) ? "the ids of " + description : null);
	}
	
	@Override
	public void putFunction(final String name, final String label, final String description,
			final Object function) {
		if (getLanguage().supportDirectModelObject()) {
			add(name, label, description);
		}
	}
	
	
	public Map<String, IContentProposal> getVariables() {
		return this.variables;
	}
	
}
