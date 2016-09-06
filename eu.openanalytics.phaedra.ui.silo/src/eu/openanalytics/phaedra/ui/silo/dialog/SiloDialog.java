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

import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.util.SiloComposite;

public class SiloDialog extends TitleAreaDialog {

	private String title;
	private String operation;

	private Silo silo;
	private boolean isExisting;
	private boolean hasParent;

	private String oldName;
	private String oldDescription;
	private AccessScope oldScope;
	private boolean oldIsExample;

	public SiloDialog(Shell parentShell, ProtocolClass pClass, GroupType type) {
		super(parentShell);
		if (type == null) type = GroupType.WELL;
		this.silo = SiloService.getInstance().createSilo(pClass, type);
	}

	public SiloDialog(Shell parentShell, Silo silo) {
		super(parentShell);

		this.silo = silo;
		this.isExisting = true;
		this.hasParent = silo.getSiloGroups() != null && !silo.getSiloGroups().isEmpty();

		this.oldName = silo.getName();
		this.oldDescription = silo.getDescription();
		this.oldScope = silo.getAccessScope();
		this.oldIsExample = silo.isExample();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		update();
		newShell.setText(title);
		newShell.setSize(450, 400);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite)super.createDialogArea(parent);
		SiloComposite container = new SiloComposite(dialogArea, SWT.NONE, silo, isExisting, hasParent);
		container.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getButton(IDialogConstants.OK_ID).setEnabled(!silo.getName().isEmpty());
			}
		});

		setTitle(title);
		setMessage("Configure the properties of the new silo.");
		return parent;
	}

	@Override
	protected void okPressed() {
		try {
			SiloService.getInstance().updateSilo(silo);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Error", "Failed to " + operation + " silo:\n" + e.getMessage());
			return;
		}
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		if (isExisting) {
			silo.setName(oldName);
			silo.setDescription(oldDescription);
			silo.setAccessScope(oldScope);
			silo.setExample(oldIsExample);
		}
		super.cancelPressed();
	}

	public Silo getSilo() {
		return silo;
	}

	private void update() {
		if (isExisting) {
			title = "Modify Silo";
			operation = "modify";
		} else {
			title = "Create New Silo";
			operation = "create";
		}
	}

}