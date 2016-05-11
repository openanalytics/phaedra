package eu.openanalytics.phaedra.base.ui.util.pref;

import static eu.openanalytics.phaedra.base.ui.util.pref.Prefs.HIGHTLIGHT_COLOR_1;
import static eu.openanalytics.phaedra.base.ui.util.pref.Prefs.HIGHTLIGHT_COLOR_2;
import static eu.openanalytics.phaedra.base.ui.util.pref.Prefs.HIGHTLIGHT_LINE_WIDTH;
import static eu.openanalytics.phaedra.base.ui.util.pref.Prefs.HIGHTLIGHT_STYLE;

import java.util.Arrays;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.ui.util.Activator;
import eu.openanalytics.phaedra.base.ui.util.highlighter.HightlightStyle;

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {

	private Combo highlightStyleCombo;
	private ColorSelector highlightColor1;
	private ColorSelector highlightColor2;
	private Spinner lineWidthSpinner;

	private String[] styleTypes;

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected Control createContents(Composite parent) {
		initializeDataArrays();
		final Composite comp = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(comp);
		buildComponents(comp);
		return comp;
	}

	private void buildComponents(final Composite comp) {
		IPreferenceStore store = getPreferenceStore();

		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText("Highlight style:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lbl);

		highlightStyleCombo = new Combo(comp, SWT.READ_ONLY);
		highlightStyleCombo.setItems(styleTypes);
		highlightStyleCombo.select(Arrays.asList(styleTypes).indexOf(store.getString(HIGHTLIGHT_STYLE)));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(highlightStyleCombo);

		lbl = new Label(comp, SWT.NONE);
		lbl.setText("Highlight color 1:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lbl);

		highlightColor1 = new ColorSelector(comp);
		highlightColor1.setColorValue(PreferenceConverter.getColor(store, Prefs.HIGHTLIGHT_COLOR_1));

		lbl = new Label(comp, SWT.NONE);
		lbl.setText("Highlight color 2:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lbl);

		highlightColor2 = new ColorSelector(comp);
		highlightColor2.setColorValue(PreferenceConverter.getColor(store, Prefs.HIGHTLIGHT_COLOR_2));

		lbl = new Label(comp,SWT.NONE);
		lbl.setText("Highlight line width:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lbl);

		lineWidthSpinner = new Spinner(comp, SWT.BORDER);
		lineWidthSpinner.setDigits(0);
		lineWidthSpinner.setMinimum(1);
		lineWidthSpinner.setMaximum(3);
		lineWidthSpinner.setIncrement(1);
		lineWidthSpinner.setSelection(store.getInt(HIGHTLIGHT_LINE_WIDTH));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lineWidthSpinner);
	}

	private void initializeDataArrays() {
		styleTypes = new String[] {
			HightlightStyle.FLASH.getName()
			, HightlightStyle.ROTATING.getName()
			, HightlightStyle.STATIC.getName()
		};
	}

	@Override
    protected void performDefaults() {
        IPreferenceStore store = getPreferenceStore();
        highlightStyleCombo.select(Arrays.asList(styleTypes).indexOf(store.getString(HIGHTLIGHT_STYLE)));
        highlightColor1.setColorValue(PreferenceConverter.getDefaultColor(store, HIGHTLIGHT_COLOR_1));
        highlightColor2.setColorValue(PreferenceConverter.getDefaultColor(store, HIGHTLIGHT_COLOR_2));
        lineWidthSpinner.setSelection(store.getDefaultInt(HIGHTLIGHT_LINE_WIDTH));
    }

	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		store.setValue(HIGHTLIGHT_STYLE, highlightStyleCombo.getText());
		PreferenceConverter.setValue(store, HIGHTLIGHT_COLOR_1, highlightColor1.getColorValue());
		PreferenceConverter.setValue(store, HIGHTLIGHT_COLOR_2, highlightColor2.getColorValue());
		store.setValue(HIGHTLIGHT_LINE_WIDTH, lineWidthSpinner.getSelection());
		return true;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}
