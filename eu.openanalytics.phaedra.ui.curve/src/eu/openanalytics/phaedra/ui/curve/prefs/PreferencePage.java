package eu.openanalytics.phaedra.ui.curve.prefs;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.ui.curve.Activator;

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
		addField(new BooleanFieldEditor(Prefs.CRC_SHOW_WEIGHTS, "Show point weights", getFieldEditorParent()));
		
		addField(new BooleanFieldEditor(Prefs.CRC_SHOW_CONF_AREA, "Show confidence area", getFieldEditorParent()));
		editor = new IntegerFieldEditor(Prefs.CRC_CONF_AREA_ALPHA, "Confidence area opacity", getFieldEditorParent());
		editor.setValidRange(0, 100);
		addField(editor);
		addField(new ColorFieldEditor(Prefs.CRC_CONF_AREA_COLOR, "Confidence area color", getFieldEditorParent()));
		
		editor = new IntegerFieldEditor(Prefs.CRC_POINT_SIZE, "Point size", getFieldEditorParent());
		editor.setValidRange(1, 100);
		addField(editor);
		addField(new ColorFieldEditor(Prefs.CRC_POINT_COLOR_ACCEPTED, "Accepted point color", getFieldEditorParent()));
		addField(new ColorFieldEditor(Prefs.CRC_POINT_COLOR_REJECTED, "Rejected point color", getFieldEditorParent()));
		
		editor = new IntegerFieldEditor(Prefs.CRC_BOUND_THICKNESS, "Lower/upper bound thickness", getFieldEditorParent());
		editor.setValidRange(1, 10);
		addField(editor);
		addField(new ColorFieldEditor(Prefs.CRC_BOUND_COLOR_LOWER, "Lower bound color", getFieldEditorParent()));
		addField(new ColorFieldEditor(Prefs.CRC_BOUND_COLOR_UPPER, "Upper bound color", getFieldEditorParent()));
		
		addField(new ColorFieldEditor(Prefs.CRC_CURVE_COLOR, "Curve color", getFieldEditorParent()));
		editor = new IntegerFieldEditor(Prefs.CRC_CURVE_THICKNESS, "Curve thickness", getFieldEditorParent());
		editor.setValidRange(1, 10);
		addField(editor);
		
	}

}
