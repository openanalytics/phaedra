package eu.openanalytics.phaedra.ui.wellimage.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.ScaleFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.ui.wellimage.Activator;

public class ImagePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private Scale thumbnailSizeScale;

	public ImagePreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected void createFieldEditors() {
		Group group = new Group(getFieldEditorParent(), SWT.SHADOW_ETCHED_IN);
		group.setText("Well Image View");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

		/*
		 * Minimum value is set again because the Scale component doesn't allow setting a minimum value lower
		 * than the current maximum value which is 100 by default.
		 */
		ScaleFieldEditor editor = new ScaleFieldEditor(Prefs.MAX_THUMBNAIL_SIZE, "Maximum thumbnail size:", group, 150, 1000, 50, 100);
		thumbnailSizeScale = editor.getScaleControl();
		thumbnailSizeScale.setMinimum(150);
		thumbnailSizeScale.addListener(SWT.Selection, e -> thumbnailSizeScale.setToolTipText("" + thumbnailSizeScale.getSelection()));


		addField(editor);
		addField(new BooleanFieldEditor(Prefs.AUTO_MOVE_CURSOR, "Automatically re-position cursor when zooming", group));


		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(group);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	protected void initialize() {
		super.initialize();
		thumbnailSizeScale.setToolTipText("" + thumbnailSizeScale.getSelection());
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		thumbnailSizeScale.setToolTipText("" + thumbnailSizeScale.getSelection());
	}

}
