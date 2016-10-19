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

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.Activator;

public class EditPlateDialog extends TitleAreaDialog {

	private Plate plate;
	
	private Text sequenceTxt;
	private Text barcodeTxt;
	private Text descriptionTxt;
	
	public EditPlateDialog(Shell parentShell, Plate plate) {
		super(parentShell);
		this.plate = plate;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Plate");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {

		// main of the whole dialog box
		Composite area = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).spacing(0,0).applyTo(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);
		
		// main of the main part of the dialog (Input)
		Composite main = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("Sequence:");
		
		sequenceTxt = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(sequenceTxt);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Barcode:");
		
		barcodeTxt = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(barcodeTxt);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Description:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		descriptionTxt = new Text(main, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(descriptionTxt);
		
		setTitle("Edit Plate");
		setMessage("You can change the properties of the plate below."
				+ "\nNote: changing a barcode does not trigger an automatic link or recalculation.");

		if (plate != null) {
			String desc = plate.getDescription();
			String bc = plate.getBarcode();
			int seq = plate.getSequence();
			
			if (desc != null) descriptionTxt.setText(desc);
			if (bc != null) barcodeTxt.setText(bc);
			sequenceTxt.setText("" + seq);
		}
		
		return main;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			close();
			return;
		}

		if (buttonId == IDialogConstants.OK_ID) {
			if (updatePlate()) close();
			return;
		}
		
		super.buttonPressed(buttonId);
	}
	
	private boolean updatePlate() {
		int sequence = 0;
		String barcode = barcodeTxt.getText();
		String description = StringUtils.trim(descriptionTxt.getText(), 200).trim();
		
		try {
			sequence = Integer.parseInt(sequenceTxt.getText());
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Update failed", "Cannot update plate: invalid plate sequence.");
			return false;
		}
		
		int maxBarcodeLength = 64;
		if (barcode.length() == 0 || barcode.length() > maxBarcodeLength) {
			MessageDialog.openError(getShell(), "Update failed", "Cannot update plate: the barcode must be between 1 and " + maxBarcodeLength + " characters long.");
			return false;
		}

		try {
			plate.setDescription(description);
			plate.setBarcode(barcode);
			plate.setSequence(sequence);
			PlateService.getInstance().updatePlate(plate);
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Update failed", "Failed to update plate: " + e.getMessage());
			EclipseLog.error("Failed to update plate", e, Activator.getDefault());
			return false;
		}
		
		return true;
	}
}