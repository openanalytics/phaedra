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
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class EditWellDescriptionDialog extends TitleAreaDialog {

	private Well well;
	private Text descriptionTxt;
	
	public EditWellDescriptionDialog(Shell parentShell, Well well) {
		super(parentShell);
		this.well = well;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Well Description");
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
		
		setTitle("Edit Well Description");
		setMessage("Enter or modify the description for " + well);

		if (well != null && well.getDescription() != null) {
			descriptionTxt.setText(well.getDescription());
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
			well.setDescription(description);
			PlateService.getInstance().updatePlate(well.getPlate());
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Update failed", "Failed to update well: " + e.getMessage());
			GenericEntityService.getInstance().refreshEntity(well);
			return false;
		}
		return true;
	}
}
