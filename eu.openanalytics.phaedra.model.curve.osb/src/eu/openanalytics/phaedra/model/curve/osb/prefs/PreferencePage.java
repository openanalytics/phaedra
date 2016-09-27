package eu.openanalytics.phaedra.model.curve.osb.prefs;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.model.curve.osb.Activator;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePage() {
		super(GRID);
	}
	
	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	protected void createFieldEditors() {
		IntegerFieldEditor editor;
		
		addField(new BooleanFieldEditor(Prefs.CRC_SHOW_PIC50_MARKER, "Show pIC50 marker", getFieldEditorParent()));
		addField(new BooleanFieldEditor(Prefs.CRC_SHOW_OTHER_IC_MARKERS, "Show pIC20/80 markers", getFieldEditorParent()));
		
		addField(new BooleanFieldEditor(Prefs.CRC_SHOW_CONF_AREA, "Show confidence area", getFieldEditorParent()));
		editor = new IntegerFieldEditor(Prefs.CRC_CONF_AREA_ALPHA, "Confidence area opacity", getFieldEditorParent());
		editor.setValidRange(0, 100);
		addField(editor);
		addField(new ColorFieldEditor(Prefs.CRC_CONF_AREA_COLOR, "Confidence area color", getFieldEditorParent()));
		
		editor = new IntegerFieldEditor(Prefs.CRC_BOUND_THICKNESS, "Lower/upper bound thickness", getFieldEditorParent());
		editor.setValidRange(1, 10);
		addField(editor);
		addField(new ColorFieldEditor(Prefs.CRC_BOUND_COLOR_LOWER, "Lower bound color", getFieldEditorParent()));
		addField(new ColorFieldEditor(Prefs.CRC_BOUND_COLOR_UPPER, "Upper bound color", getFieldEditorParent()));
	}

}
