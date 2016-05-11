package eu.openanalytics.phaedra.base.ui.nattable.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.ui.nattable.Activator;

public class NatTablePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public NatTablePreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		addField(
				new BooleanFieldEditor(
						Prefs.INC_COLUMN_HEADER_AUTO_RESIZE
						, "Auto-resize: Take column header into account"
						, getFieldEditorParent()
				)
		);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}
