package eu.openanalytics.phaedra.base.ui.gridviewer.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.ui.gridviewer.Activator;

public class GridPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public GridPreferencePage() {		
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Nothing to do.
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(Prefs.GRID_TOOLTIPS, "Show Tooltips for Grid Layers", getFieldEditorParent()));
	}

	@Override
	protected Control createContents(Composite parent) {
		return super.createContents(parent);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}