package eu.openanalytics.phaedra.ui.plate.dialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.environment.GenericEntityService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;

public class EditCompoundDescriptionDialog extends TitleAreaDialog {

	private Compound compound;
	private Text descriptionTxt;
	
	public EditCompoundDescriptionDialog(Shell parentShell, Compound compound) {
		super(parentShell);
		this.compound = compound;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Compound Description");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {

		// main of the whole dialog box
		Composite area = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);

		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Description:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		descriptionTxt = new Text(area, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(descriptionTxt);
		
		setTitle("Edit Compound Description");
		setMessage("Enter or modify the description for compound " + compound);

		if (compound != null && compound.getDescription() != null) {
			descriptionTxt.setText(compound.getDescription());
		}
		
		return area;
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			if (updateWell()) close();
			return;
		}
		else super.buttonPressed(buttonId);
	}
	
	private boolean updateWell() {
		String description = descriptionTxt.getText();
		try {
			compound.setDescription(description);
			PlateService.getInstance().updatePlate(compound.getPlate());
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Update failed", "Failed to update compound: " + e.getMessage());
			GenericEntityService.getInstance().refreshEntity(compound);
			return false;
		}
		return true;
	}
}