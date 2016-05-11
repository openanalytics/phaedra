package eu.openanalytics.phaedra.base.fs.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.fs.Activator;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePage() {
		super(GRID);
	}
	
	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = (Composite)super.createContents(parent);
		Label infoLabel = new Label(container, SWT.NONE);
		infoLabel = new Label(container, SWT.NONE);
		infoLabel = new Label(container, SWT.NONE);
		infoLabel.setText("*: Requires restart");
		return container;
	}
	
	@Override
	protected void createFieldEditors() {
		addField(new IntegerFieldEditor(
				Prefs.SMB_SOCKET_TIMEOUT, "SMB Socket Timeout (ms) *:", getFieldEditorParent()));
		addField(new IntegerFieldEditor(
				Prefs.SMB_RESPONSE_TIMEOUT, "SMB Response Timeout (ms) *:", getFieldEditorParent()));
		addField(new IntegerFieldEditor(
				Prefs.UPLOAD_RETRIES, "Max Upload Tries:", getFieldEditorParent()));
		addField(new IntegerFieldEditor(
				Prefs.UPLOAD_RETRY_DELAY, "Delay Between Upload Tries (ms):", getFieldEditorParent()));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}
