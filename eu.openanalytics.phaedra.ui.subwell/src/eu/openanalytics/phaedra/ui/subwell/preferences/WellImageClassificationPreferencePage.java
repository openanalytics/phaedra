package eu.openanalytics.phaedra.ui.subwell.preferences;



import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.ScaleFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.ui.subwell.Activator;

public class WellImageClassificationPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public WellImageClassificationPreferencePage() {
		super(FLAT);
	}

	@Override
	protected void createFieldEditors() {
		int maxValue = 5;
		String[][] sizeLabelsAndValues = new String[maxValue][2];
		for (int i = 0; i < maxValue; i++) {
			sizeLabelsAndValues[i][0] = Integer.toString(i + 1);
			sizeLabelsAndValues[i][1] = Integer.toString(i + 1);
		}

		String[][] distanceLabelsAndValues = new String[maxValue+ 1][2];
		for (int i = 0; i <= maxValue; i++) {
			distanceLabelsAndValues[i][0] = Integer.toString(i);
			distanceLabelsAndValues[i][1] = Integer.toString(i);
		}
		
		int[] largeSizeValues = {1,2,3,4,5,10,15,20,25,30};		
		String[][] largeSizeLabelsAndValues = new String[largeSizeValues.length][2];
		for (int i = 0; i < largeSizeValues.length; i++) {
			largeSizeLabelsAndValues[i][0] = largeSizeValues[i] + "";
			largeSizeLabelsAndValues[i][1] = largeSizeValues[i] + "";
		}

		String[][] styleLabelsAndValues = new String[5][2];
		styleLabelsAndValues[0][1] = SWT.LINE_DASH + "";
		styleLabelsAndValues[0][0] = "Dash";
		styleLabelsAndValues[1][1] = SWT.LINE_DASHDOT + "";
		styleLabelsAndValues[1][0] = "Dash dot";
		styleLabelsAndValues[2][1] = SWT.LINE_DASHDOTDOT + "";
		styleLabelsAndValues[2][0] = "Dash dot dot";
		styleLabelsAndValues[3][1] = SWT.LINE_DOT + "";
		styleLabelsAndValues[3][0] = "Dot";
		styleLabelsAndValues[4][1] = SWT.LINE_SOLID + "";
		styleLabelsAndValues[4][0] = "Solid";

		Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);
		
		Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
		group.setText("Classification");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		
		addField(new ComboFieldEditor(Prefs.CLASSIFICATION_SYMBOL_LINE_WIDTH, "Icon line width:", sizeLabelsAndValues, group));
		addField(new ComboFieldEditor(Prefs.CLASSIFICATION_SYMBOL_SIZE, "Icon size:", largeSizeLabelsAndValues, group));
		addField(new ScaleFieldEditor(Prefs.CLASSIFICATION_SYMBOL_OPACITY, "Opacity:", group, 0, 255, 1, 51));
		addField(new BooleanFieldEditor(Prefs.CLASSIFICATION_SYMBOL_FILL, "Fill icon", group));
		new Label(group, SWT.NONE);
		
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(group);
		
		group = new Group(parent, SWT.SHADOW_ETCHED_IN);
		group.setText("Selection");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		
		addField(new ColorFieldEditor(Prefs.CLASSIFICATION_SELECTION_LINE_COLOR, "Icon color:", group));
		addField(new ComboFieldEditor(Prefs.CLASSIFICATION_SELECTION_LINE_WIDTH, "Icon line width:", sizeLabelsAndValues,group));
		addField(new ComboFieldEditor(Prefs.CLASSIFICATION_SELECTION_LINE_STYLE, "Icon line style:", styleLabelsAndValues, group));
		addField(new ComboFieldEditor(Prefs.CLASSIFICATION_SELECTION_LINE_DISTANCE, "Icon padding:", largeSizeLabelsAndValues, group));
		addField(new ScaleFieldEditor(Prefs.CLASSIFICATION_SELECTION_OPACITY, "Opacity:", group, 0, 255, 1, 51));
		addField(new BooleanFieldEditor(Prefs.CLASSIFICATION_SELECTION_LINE_OUTER, "Wrap classification icon", group));
		new Label(group, SWT.NONE);
		addField(new BooleanFieldEditor(Prefs.CLASSIFICATION_SELECTION_SHAPE, "Use classification shape", group));
		
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(group);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}
