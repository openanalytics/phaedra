package eu.openanalytics.phaedra.ui.plate.preferences;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.ui.plate.Activator;


public class SvgchartPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public SvgchartPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		String[][] labelsAndValues = {
				new String[] { "No background color (transparent)", "true" },
				new String[] { "Background color:", "false" },
		};
		addField(new RadioGroupFieldEditor(Prefs.SVG_NO_BG, "Chart background:", 1, labelsAndValues, getFieldEditorParent()));
		addField(new ColorFieldEditor(Prefs.SVG_BG_COLOR, "Color:", getFieldEditorParent()));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}
