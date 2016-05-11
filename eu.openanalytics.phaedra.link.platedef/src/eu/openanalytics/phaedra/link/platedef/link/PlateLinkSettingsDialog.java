package eu.openanalytics.phaedra.link.platedef.link;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Shell;


public abstract class PlateLinkSettingsDialog extends TitleAreaDialog {

	public PlateLinkSettingsDialog(Shell parentShell) {
		super(parentShell);
	}

	public abstract PlateLinkSettings getSettings();
}
