package eu.openanalytics.phaedra.ui.wellimage.preferences;

import static eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.SCALE_RATIOS_PART1;
import static eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.SCALE_RATIOS_PART2;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.ui.wellimage.Activator;

public class ImageToolTipPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ImageToolTipPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(Prefs.SHOW_IMAGE_TOOLTIP, "Show Image in Tooltip", getFieldEditorParent()));
		addField(new BooleanFieldEditor(Prefs.SHOW_TEXT_TOOLTIP, "Show Text in Tooltip", getFieldEditorParent()));
		addField(new BooleanFieldEditor(Prefs.SHOW_ADVANCED_TOOLTIP, "Show Advanced items in Tooltip", getFieldEditorParent()));

		int size = Math.max(SCALE_RATIOS_PART1.length, SCALE_RATIOS_PART2.length);
		String[][] labelsAndValues = new String[size][2];

		for (int i = 0; i < size; i++) {
			labelsAndValues[i][0] = SCALE_RATIOS_PART1[i] + ":" + SCALE_RATIOS_PART2[i];
			labelsAndValues[i][1] = "" + (float)SCALE_RATIOS_PART1[i]/SCALE_RATIOS_PART2[i];
		}

		addField(
				new ComboFieldEditor(
						Prefs.WELL_IMAGE_TOOLTIP_SCALE
						, "Well Image Scale:"
						, labelsAndValues
						, getFieldEditorParent()
				)
		);
		
		addField(
				new ComboFieldEditor(
						Prefs.SUBWELL_IMAGE_TOOLTIP_SCALE
						, "Subwell Image Scale:"
						, labelsAndValues
						, getFieldEditorParent()
				)
		);
		
		addField(new IntegerFieldEditor(Prefs.IMAGE_TOOLTIP_MAX_X, "Maximum Tooltip Size (x):", getFieldEditorParent()));
		addField(new IntegerFieldEditor(Prefs.IMAGE_TOOLTIP_MAX_Y, "Maximum Tooltip Size (y):", getFieldEditorParent()));
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
