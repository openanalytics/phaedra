package eu.openanalytics.phaedra.ui.plate.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.ui.plate.Activator;

public class MultiFeatureLayerPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public MultiFeatureLayerPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {

		Composite parent = getFieldEditorParent();

		addField(new IntegerFieldEditor(Prefs.MULTI_FEATURE_MIN_SIZE, "Minimum size:", parent, 3));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}