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

import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.util.EditSiloComposite;

public class CreateSiloDialog extends TitleAreaDialog {

	private Silo silo;

	public CreateSiloDialog(Shell parent, ProtocolClass pClass, GroupType type) {
		super(parent);
		if (type == null) type = GroupType.WELL;
		this.silo = SiloService.getInstance().createSilo(pClass, type);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Create New Silo");
		newShell.setSize(450, 400);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite)super.createDialogArea(parent);
		EditSiloComposite container = new EditSiloComposite(dialogArea, SWT.NONE, silo, true, true, true);
		container.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getButton(IDialogConstants.OK_ID).setEnabled(!silo.getName().isEmpty());
			}
		});

		setTitle("Create New Silo");
		setMessage("Configure the properties of the new silo.");
		return parent;
	}

	@Override
	protected void okPressed() {
		try {
			SiloService.getInstance().updateSilo(silo);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Error", "Failed to save silo:\n" + e.getMessage());
			return;
		}
		super.okPressed();
	}

	public Silo getSilo() {
		return silo;
	}
}