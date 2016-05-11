package eu.openanalytics.phaedra.ui.plate.preferences;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.grid.layer.ValueLayer;
import eu.openanalytics.phaedra.ui.plate.grid.layer.ValueProvider;
import eu.openanalytics.phaedra.ui.plate.grid.layer.ValueProvider.ValueKey;
import eu.openanalytics.phaedra.ui.plate.grid.layer.config.ValueConfig;

public class ValueLayerPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ValueLayerPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		WellProperty[] types = WellProperty.values();
		String[][] labelsAndValues = new String[types.length + 2][2];
		labelsAndValues[0][0] = ValueProvider.VALUE_TYPE_ACTIVE_FEATURE;
		labelsAndValues[0][1] = ValueKey.create(ValueProvider.VALUE_TYPE_ACTIVE_FEATURE).toIdString();
		for (int i=0; i<types.length; i++) {
			labelsAndValues[i+1][0] = types[i].getLabel();
			labelsAndValues[i+1][1] = ValueKey.create(ValueProvider.VALUE_TYPE_PROPERTY, types[i]).toIdString();
		}
		labelsAndValues[types.length+1][0] = ValueProvider.VALUE_TYPE_NONE;
		labelsAndValues[types.length+1][1] = ValueKey.create(ValueProvider.VALUE_TYPE_NONE).toIdString();

		addField(new ComboFieldEditor(Prefs.DEFAULT_HEATMAP_LABEL_1,
				"Default label 1:", labelsAndValues, getFieldEditorParent()));

		addField(new ComboFieldEditor(Prefs.DEFAULT_HEATMAP_LABEL_2,
				"Default label 2:", labelsAndValues, getFieldEditorParent()));

		addField(new ComboFieldEditor(Prefs.DEFAULT_HEATMAP_LABEL_3,
				"Default label 3:", labelsAndValues, getFieldEditorParent()));

		addField(new FontFieldEditor(Prefs.HEATMAP_FONT,
				"Font:", getFieldEditorParent()));

		labelsAndValues = new String[][] {
				{"Based on heatmap color", ""+ValueConfig.FONT_COLOR_AUTO},
				{"Black", ""+ValueConfig.FONT_COLOR_BLACK},
				{"White", ""+ValueConfig.FONT_COLOR_WHITE}
		};
		addField(new ComboFieldEditor(Prefs.HEATMAP_FONT_COLOR,
				"Font color:", labelsAndValues, getFieldEditorParent()));
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);

		Button resetButton = new Button(parent, SWT.NONE);
		resetButton.setText("Reset to preferences");
		resetButton.addListener(SWT.MouseDown, e -> {
			GridState.removeValue(-1, ValueLayer.class.getName(), ValueConfig.SETTING_LABEL_1);
			GridState.removeValue(-1, ValueLayer.class.getName(), ValueConfig.SETTING_LABEL_2);
			GridState.removeValue(-1, ValueLayer.class.getName(), ValueConfig.SETTING_LABEL_3);
			GridState.removeValue(-1, ValueLayer.class.getName(), ValueConfig.SETTING_FONT_COLOR);
		});
		resetButton.setOrientation(SWT.LEFT_TO_RIGHT);

		return control;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}