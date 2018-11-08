package eu.openanalytics.phaedra.ui.perspective.dialog;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.security.model.AccessScope;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.ui.perspective.Activator;
import eu.openanalytics.phaedra.ui.perspective.PerspectiveService;
import eu.openanalytics.phaedra.ui.perspective.vo.SavedPerspective;

public class PerspectiveSettingsDialog extends TitleAreaDialog {

	private SavedPerspective perspective;
	private boolean editable;
	
	private Text nameTxt;
	private Text idTxt;
	private Text ownerTxt;
	private Combo accessCmb;
	
	public PerspectiveSettingsDialog(Shell parentShell, SavedPerspective perspective) {
		super(parentShell);
		this.perspective = perspective;
		this.editable = PerspectiveService.getInstance().canUpdatePerspective(perspective);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Saved Perspective Settings");
		newShell.setSize(450, 300);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		// Container of the whole dialog box
		Composite area = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(area);

		new Label(area, SWT.NONE).setText("Name:");
		nameTxt = new Text(area, SWT.BORDER);
		nameTxt.setText(perspective.getName());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameTxt);
		
		new Label(area, SWT.NONE).setText("Id:");
		idTxt = new Text(area, SWT.BORDER);
		idTxt.setEditable(false);
		idTxt.setText(perspective.getId() + "");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(idTxt);
		
		new Label(area, SWT.NONE).setText("Owner:");
		ownerTxt = new Text(area, SWT.BORDER);
		ownerTxt.setEditable(false);
		ownerTxt.setText(perspective.getOwner());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(ownerTxt);
		
		new Label(area, SWT.NONE).setText("Access:");
		accessCmb = new Combo(area, SWT.READ_ONLY);
		accessCmb.setItems(AccessScope.getScopeNames());
		accessCmb.select(CollectionUtils.find(AccessScope.getScopeNames(), perspective.getAccessScope().getName()));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(accessCmb);
		
		setTitle("Saved Perspective Settings");
		setMessage("If you have the required permissions, you can modify the settings of the Saved Perspective below.");
		
		return area;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button okButton = createButton(parent, IDialogConstants.OK_ID, "OK", true);
		okButton.setEnabled(editable);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}
	
	@Override
	protected void okPressed() {
		try {
			if (!editable) throw new IllegalStateException("Cannot update PSP: no permission");
			perspective.setName(nameTxt.getText());
			perspective.setAccessScope(AccessScope.values()[accessCmb.getSelectionIndex()]);
			PerspectiveService.getInstance().savePerspectiveSettings(perspective);
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Update failed", "Failed to update the PSP", new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
		}
		super.okPressed();
	}
}
