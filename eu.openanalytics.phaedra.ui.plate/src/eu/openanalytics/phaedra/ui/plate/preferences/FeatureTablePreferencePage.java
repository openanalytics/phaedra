package eu.openanalytics.phaedra.ui.plate.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.ui.plate.Activator;

public class FeatureTablePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public FeatureTablePreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(Prefs.WELL_TABLE_COLORS,
				"Use heatmap colors in well table", getFieldEditorParent()));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}
