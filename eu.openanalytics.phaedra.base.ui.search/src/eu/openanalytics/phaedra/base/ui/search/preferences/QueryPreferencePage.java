package eu.openanalytics.phaedra.base.ui.search.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.search.Activator;
import eu.openanalytics.phaedra.base.search.preferences.Prefs;

public class QueryPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public QueryPreferencePage() {
		super(GRID);
	}
	
	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		addField(new IntegerFieldEditor(Prefs.DEFAULT_MAX_RESULTS, "Default maximum results:", getFieldEditorParent()));		
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}
