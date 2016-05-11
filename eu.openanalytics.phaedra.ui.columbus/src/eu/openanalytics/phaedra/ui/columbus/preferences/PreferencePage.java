package eu.openanalytics.phaedra.ui.columbus.preferences;


import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.ui.columbus.Activator;


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
		addField(new StringFieldEditor(
				Prefs.DEFAULT_SOURCE_PATH, "Default OperaDB path:", getFieldEditorParent()));
		addField(new StringFieldEditor(
				Prefs.DEFAULT_IMAGE_PATH, "Default image data path:", getFieldEditorParent()));
		addField(new StringFieldEditor(
				Prefs.DEFAULT_SUBWELL_DATA_PATH, "Default subwell data path:", getFieldEditorParent()));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}