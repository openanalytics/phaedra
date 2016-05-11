package eu.openanalytics.phaedra.wellimage.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.wellimage.Activator;

public class SubWellImagePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Button isAbsolute;
	private Button isRelative;
	private Spinner absPadding;
	private Spinner relPadding;

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
		buildComponents(composite);
		return composite;
	}

	private void buildComponents(Composite parent) {
		IPreferenceStore store = getPreferenceStore();

		Group grp;
		Label lbl;

		grp = new Group(parent, SWT.NONE);
		grp.setText("Padding");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grp);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(grp);

		Listener listener = e -> {
			boolean isAbs = isAbsolute.getSelection();
			setAbsolute(isAbs);
		};

		isAbsolute = new Button(grp, SWT.RADIO);
		isAbsolute.setText("Absolute Padding");
		isAbsolute.addListener(SWT.Selection, listener);
		isAbsolute.setSelection(store.getBoolean(Prefs.SW_IMG_IS_ABS_PADDING));

		isRelative = new Button(grp, SWT.RADIO);
		isRelative.setText("Relative Padding");
		isRelative.setSelection(!store.getBoolean(Prefs.SW_IMG_IS_ABS_PADDING));

		lbl = new Label(grp, SWT.NONE);
		lbl.setText("Absolute Padding (in px):");

		absPadding = new Spinner(grp, SWT.BORDER);
		absPadding.setMinimum(-5);
		absPadding.setMaximum(50);
		absPadding.setIncrement(1);
		absPadding.setPageIncrement(5);
		absPadding.setSelection(store.getInt(Prefs.SW_IMG_ABS_PADDING));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(absPadding);

		lbl = new Label(grp, SWT.NONE);
		lbl.setText("Relative Padding (in %):");

		relPadding = new Spinner(grp, SWT.BORDER);
		relPadding.setMinimum(-5);
		relPadding.setMaximum(50);
		relPadding.setIncrement(1);
		relPadding.setPageIncrement(10);
		relPadding.setSelection(store.getInt(Prefs.SW_IMG_REL_PADDING));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(relPadding);

		setAbsolute(store.getBoolean(Prefs.SW_IMG_IS_ABS_PADDING));
	}

	private void setAbsolute(boolean isAbs) {
		absPadding.setEnabled(isAbs);
		relPadding.setEnabled(!isAbs);
	}

	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		store.setValue(Prefs.SW_IMG_IS_ABS_PADDING, isAbsolute.getSelection());
		store.setValue(Prefs.SW_IMG_ABS_PADDING, absPadding.getSelection());
		store.setValue(Prefs.SW_IMG_REL_PADDING, relPadding.getSelection());
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();
		isAbsolute.setSelection(store.getDefaultBoolean(Prefs.SW_IMG_IS_ABS_PADDING));
		isRelative.setSelection(!store.getDefaultBoolean(Prefs.SW_IMG_IS_ABS_PADDING));
		absPadding.setSelection(store.getDefaultInt(Prefs.SW_IMG_ABS_PADDING));
		relPadding.setSelection(store.getDefaultInt(Prefs.SW_IMG_REL_PADDING));
		super.performDefaults();
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}
