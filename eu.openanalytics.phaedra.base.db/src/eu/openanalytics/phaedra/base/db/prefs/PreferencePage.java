package eu.openanalytics.phaedra.base.db.prefs;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.db.Activator;
import eu.openanalytics.phaedra.base.util.misc.VersionUtils;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		addField(new IntegerFieldEditor(Prefs.DB_TIME_OUT, "Connection timeout:", parent));
		String phaedraVersion = VersionUtils.getPhaedraVersion();
		if (phaedraVersion.equals(VersionUtils.UNKNOWN) || phaedraVersion.toLowerCase().contains("dev")) {
//			Label lbl = new Label(parent, SWT.NONE);
//			lbl.setText("Advanced Settings:");
//			Button btn = new Button(parent, SWT.CHECK);

			IntegerFieldEditor dbPoolSize = new IntegerFieldEditor(Prefs.DB_POOL_SIZE, "Database pool size:", parent, 2);
//			dbPoolSize.setEnabled(false, parent);
			dbPoolSize.setValidRange(1, 12);
			addField(dbPoolSize);

//			btn.addListener(SWT.Selection, e -> {
//				dbPoolSize.setEnabled(btn.getSelection(), parent);
//			});
		}
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}
