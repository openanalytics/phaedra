package eu.openanalytics.phaedra.base.ui.charting.preferences;

import java.util.Arrays;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage{

	private Combo symbolSize;
	private Text chartPadding;
	private Spinner opacitySpinner;
	private ColorSelector colorSelector;
	private Combo symbolType;
	private Combo barType;
	private Button btnWellSVG;
	private Button btnSubwellSVG;
	private Button btnWellBitmap;
	private Button btnSubwellBitmap;
	private Button btnSkipZeroDensity;
	private Button btnUpdateOnFocus;

	private String[] symbolSizes;
	private String[] symbolTypes;
	private String[] barTypes;

	private boolean wellImageAsVector;
	private boolean subwellImageAsVector;

	@Override
	public void init(IWorkbench workbench) {
		//Do nothing
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

		Label lblSize = new Label(comp, SWT.NONE);
		lblSize.setText("Symbol size:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblSize);

		symbolSize = new Combo(comp, SWT.READ_ONLY);
		symbolSize.setItems(symbolSizes);
		symbolSize.select(Arrays.asList(symbolSizes).indexOf(
				store.getString(Prefs.SYMBOL_SIZE)
				));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(symbolSize);

		Label lblPadding = new Label(comp,SWT.NONE);
		lblPadding.setText("Chart padding in layers:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblPadding);

		chartPadding = new Text(comp, SWT.SINGLE | SWT.BORDER);
		chartPadding.setText(store.getString(Prefs.PADDING));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(chartPadding);

		Label lblOpacity = new Label(comp,SWT.NONE);
		lblOpacity.setText("Opacity for unselected points (0-1):");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblOpacity);

		opacitySpinner = new Spinner(comp, SWT.BORDER);
		opacitySpinner.setDigits(2);
		opacitySpinner.setMinimum(1);
		opacitySpinner.setMaximum(100);
		opacitySpinner.setIncrement(1);
		opacitySpinner.setSelection(store.getInt(Prefs.SELECTION_OPACITY));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(opacitySpinner);

		Label lblColor = new Label(comp, SWT.NONE);
		lblColor.setText("Default color for chart points:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblColor);

		colorSelector = new ColorSelector(comp);
		final RGB rgb = PreferenceConverter.getColor(store, Prefs.DEFAULT_COLOR);
		colorSelector.setColorValue(rgb);

		Label lblType = new Label(comp, SWT.NONE);
		lblType.setText("Default symbol style:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblType);

		symbolType = new Combo(comp, SWT.READ_ONLY);
		symbolType.setItems(symbolTypes);
		int indexOf = Arrays.asList(symbolTypes).indexOf(store.getString(Prefs.DEFAULT_SYMBOL_TYPE));
		symbolType.select(Math.max(0, indexOf));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(symbolType);

		lblType = new Label(comp, SWT.NONE);
		lblType.setText("Default bar style:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblType);

		barType = new Combo(comp, SWT.READ_ONLY);
		barType.setItems(barTypes);
		barType.select(Arrays.asList(barTypes).indexOf(store.getString(Prefs.DEFAULT_BAR_TYPE)));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(barType);

		lblType = new Label(comp, SWT.NONE);
		lblType.setText("Skip zero density:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblType);

		btnSkipZeroDensity = new Button(comp, SWT.CHECK);
		btnSkipZeroDensity.setSelection(store.getBoolean(Prefs.SKIP_ZERO_DENSITY));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnSkipZeroDensity);

		lblType = new Label(comp, SWT.NONE);
		lblType.setText("Update Feature on focus:");

		btnUpdateOnFocus = new Button(comp, SWT.CHECK);
		btnUpdateOnFocus.setSelection(store.getBoolean(Prefs.UPDATE_FEATURE_ON_FOCUS));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnUpdateOnFocus);

		// Exporting options
		Group grpReporting = new Group(comp, SWT.SHADOW_IN);
		grpReporting.setText("Reporting");
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(grpReporting);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(grpReporting);

		Label lblExporting = new Label(grpReporting, SWT.NONE);
		lblExporting.setText("Output format Well charts");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblExporting);

		wellImageAsVector = store.getBoolean(Prefs.EXPORT_WELL_IMAGE_AS_VECTOR);
		subwellImageAsVector = store.getBoolean(Prefs.EXPORT_SUBWELL_IMAGE_AS_VECTOR);

		Composite compSubwell = new Composite(grpReporting, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(compSubwell);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(compSubwell);
		btnWellSVG = new Button(compSubwell, SWT.RADIO);
		btnWellSVG.setText("SVG");
		btnWellSVG.setSelection(wellImageAsVector);
		btnWellBitmap = new Button(compSubwell, SWT.RADIO);
		btnWellBitmap.setText("Bitmap");
		btnWellBitmap.setSelection(!wellImageAsVector);

		lblExporting = new Label(grpReporting, SWT.NONE);
		lblExporting.setText("Output format Subwell charts");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblExporting);

		compSubwell = new Composite(grpReporting, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(compSubwell);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(compSubwell);
		btnSubwellSVG = new Button(compSubwell, SWT.RADIO);
		btnSubwellSVG.setText("SVG");
		btnSubwellSVG.setSelection(subwellImageAsVector);
		btnSubwellBitmap = new Button(compSubwell, SWT.RADIO);
		btnSubwellBitmap.setText("Bitmap");
		btnSubwellBitmap.setSelection(!subwellImageAsVector);
	}

	private void initializeDataArrays() {
		barTypes = new String[] { DefaultStyleProvider.BARSTYLE_FILLED, DefaultStyleProvider.BARSTYLE_FILLED_3D
				, DefaultStyleProvider.BARSTYLE_OPEN, DefaultStyleProvider.BARSTYLE_SPIKES, DefaultStyleProvider.BARSTYLE_STEPS };
		symbolTypes = new String[] { DefaultStyleProvider.FILLED_CIRCLE, DefaultStyleProvider.OPEN_CIRCLE
				, DefaultStyleProvider.OPEN_RECTANGLE, DefaultStyleProvider.FILLED_RECTANGLE };
		symbolSizes = new String[6];
		for (int i = 0; i < 6; i++) {
			symbolSizes[i] = i + "";
			symbolSizes[i] = i + "";
		}
	}

	@Override
    protected void performDefaults() {
        IPreferenceStore store = getPreferenceStore();
        symbolSize.select(Arrays.asList(symbolSizes).indexOf(
				store.getDefaultString(Prefs.SYMBOL_SIZE)));
        chartPadding.setText(store.getDefaultString(Prefs.PADDING));
        opacitySpinner.setSelection(store.getDefaultInt(Prefs.SELECTION_OPACITY));
        colorSelector.setColorValue(PreferenceConverter.getDefaultColor(store, Prefs.DEFAULT_COLOR));
        String type = store.getDefaultString(Prefs.DEFAULT_SYMBOL_TYPE);
        int index = Arrays.asList(symbolTypes).indexOf(type);
        symbolType.select(index);
        type = store.getDefaultString(Prefs.DEFAULT_BAR_TYPE);
        index = Arrays.asList(barTypes).indexOf(type);
        barType.select(index);
        boolean wellImageAsVectorDef = store.getDefaultBoolean(Prefs.EXPORT_WELL_IMAGE_AS_VECTOR);
        boolean subwellImageAsVectorDef = store.getDefaultBoolean(Prefs.EXPORT_SUBWELL_IMAGE_AS_VECTOR);
        btnWellSVG.setSelection(wellImageAsVectorDef);
        btnWellBitmap.setSelection(!wellImageAsVectorDef);
        btnSkipZeroDensity.setSelection(store.getDefaultBoolean(Prefs.SKIP_ZERO_DENSITY));
        btnUpdateOnFocus.setSelection(store.getDefaultBoolean(Prefs.UPDATE_FEATURE_ON_FOCUS));
        btnSubwellSVG.setSelection(subwellImageAsVectorDef);
        btnSubwellBitmap.setSelection(!subwellImageAsVectorDef);
    }

	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		store.setValue(Prefs.SYMBOL_SIZE, symbolSizes[symbolSize.getSelectionIndex()]);
		store.setValue(Prefs.PADDING, chartPadding.getText());
		store.setValue(Prefs.SELECTION_OPACITY, opacitySpinner.getSelection());
		PreferenceConverter.setValue(store, Prefs.DEFAULT_COLOR, colorSelector.getColorValue());
		store.setValue(Prefs.DEFAULT_SYMBOL_TYPE, symbolTypes[symbolType.getSelectionIndex()]);
		store.setValue(Prefs.DEFAULT_BAR_TYPE, barTypes[barType.getSelectionIndex()]);
		store.setValue(Prefs.SKIP_ZERO_DENSITY, btnSkipZeroDensity.getSelection());
		store.setValue(Prefs.UPDATE_FEATURE_ON_FOCUS, btnUpdateOnFocus.getSelection());
		store.setValue(Prefs.EXPORT_WELL_IMAGE_AS_VECTOR, btnWellSVG.getSelection());
		store.setValue(Prefs.EXPORT_SUBWELL_IMAGE_AS_VECTOR, btnSubwellSVG.getSelection());
		return true;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}
