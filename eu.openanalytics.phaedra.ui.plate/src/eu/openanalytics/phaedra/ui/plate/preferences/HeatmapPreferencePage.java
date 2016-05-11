package eu.openanalytics.phaedra.ui.plate.preferences;


import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.ui.plate.Activator;

public class HeatmapPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public HeatmapPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {

		Composite parent = getFieldEditorParent();
		
		addField(new BooleanFieldEditor(Prefs.HEATMAP_ANNOTATIONS, "Show annotation notifications on the wells", parent));

		addField(new ColorFieldEditor(Prefs.HEATMAP_ANNOTATION_WELL_COLOR, "Well annotation notification color:", parent));

		addField(new ColorFieldEditor(Prefs.HEATMAP_ANNOTATION_SUBWELL_COLOR, "Subwell annotation notification color:", parent));

		addField(new IntegerFieldEditor(Prefs.HEATMAP_ANNOTATION_SIZE, "Annotation notification size (in %):", parent, 3));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}
