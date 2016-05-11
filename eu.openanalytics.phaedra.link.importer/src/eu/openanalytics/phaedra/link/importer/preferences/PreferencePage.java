package eu.openanalytics.phaedra.link.importer.preferences;


import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.link.importer.Activator;


public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePage() {
		super(GRID);
	}
	
	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}
	
	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(
				Prefs.DETECT_WELL_FEATURES, "Prompt to create undefined well features", getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				Prefs.DETECT_SUBWELL_FEATURES, "Prompt to create undefined sub-well features", getFieldEditorParent()));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}