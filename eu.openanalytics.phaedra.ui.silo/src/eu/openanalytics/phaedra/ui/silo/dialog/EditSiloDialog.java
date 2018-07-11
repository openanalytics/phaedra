package eu.openanalytics.phaedra.ui.silo.dialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.silo.util.ObjectCopyFactory;
import eu.openanalytics.phaedra.silo.util.SiloUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.util.EditSiloComposite;

public class EditSiloDialog extends TitleAreaDialog {

	private Silo silo;
	private Silo workingCopy;

	public EditSiloDialog(Shell parentShell, Silo silo) {
		super(parentShell);
		this.silo = silo;
		this.workingCopy = new Silo();
		ObjectCopyFactory.copy(silo, workingCopy, true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Silo");
		newShell.setSize(450, 400);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite)super.createDialogArea(parent);
		EditSiloComposite container = new EditSiloComposite(dialogArea, SWT.NONE, workingCopy, false, false, true);
		container.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getButton(IDialogConstants.OK_ID).setEnabled(!workingCopy.getName().isEmpty());
			}
		});

		setTitle("Edit Silo");
		setMessage("Configure the properties of the new silo.");
		return parent;
	}

	@Override
	protected void okPressed() {
		try {
			SiloUtils.saveSiloChanges(silo, workingCopy);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Error", "Failed to save silo:\n" + e.getMessage());
			return;
		}
		super.okPressed();
	}
}