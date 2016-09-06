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
import eu.openanalytics.phaedra.silo.vo.SiloGroup;
import eu.openanalytics.phaedra.ui.silo.util.SiloGroupComposite;

public class SiloGroupDialog extends TitleAreaDialog {

	private String title;
	private String operation;

	private SiloGroup siloGroup;
	private boolean isExisting;
	private boolean hasChildren;

	public SiloGroupDialog(Shell parentShell, ProtocolClass pClass, GroupType type) {
		super(parentShell);
		if (type == null) type = GroupType.WELL;
		this.siloGroup = SiloService.getInstance().createSiloGroup(pClass, type, AccessScope.PRIVATE);
	}

	public SiloGroupDialog(Shell parentShell, SiloGroup siloGroup) {
		super(parentShell);
		this.siloGroup = siloGroup;
		this.isExisting = true;
		this.hasChildren = siloGroup.getSilos() != null && !siloGroup.getSilos().isEmpty();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		update();
		newShell.setText(title);
		newShell.setSize(450, 400);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite)super.createDialogArea(parent);
		SiloGroupComposite container = new SiloGroupComposite(dialogArea, SWT.NONE, siloGroup, hasChildren);
		container.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getButton(IDialogConstants.OK_ID).setEnabled(!siloGroup.getName().isEmpty());
			}
		});
		setTitle(title);
		setMessage("Configure the properties of the new silo group.");
		return parent;
	}

	@Override
	protected void okPressed() {
		try {
			SiloService.getInstance().updateSiloGroup(siloGroup);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Error", "Failed to " + operation + " silo group:\n" + e.getMessage());
			return;
		}
		super.okPressed();
	}

	public SiloGroup getSiloGroup() {
		return siloGroup;
	}

	private void update() {
		if (isExisting) {
			title = "Modify Silo Group";
			operation = "modify";
		} else {
			title = "Create New Silo Group";
			operation = "modify";
		}
	}

}