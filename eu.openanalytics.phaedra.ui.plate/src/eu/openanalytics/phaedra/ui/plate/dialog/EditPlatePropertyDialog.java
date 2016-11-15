package eu.openanalytics.phaedra.ui.plate.dialog;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.Activator;

public class EditPlatePropertyDialog extends TitleAreaDialog {

	private List<Plate> plates;
	
	private Combo propNameCmb;
	private Text propValueTxt;
	
	public EditPlatePropertyDialog(Shell parentShell, List<Plate> plates) {
		super(parentShell);
		this.plates = plates;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Plate Property");
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
		lbl.setText("Name:");

		propNameCmb = new Combo(main, SWT.BORDER);
		propNameCmb.addModifyListener(e -> {
			if (plates.isEmpty()) return;
			String value = PlateService.getInstance().getPlateProperty(plates.get(0), propNameCmb.getText());
			if (value == null) value = "";
			propValueTxt.setText(value);	
		});
		GridDataFactory.fillDefaults().grab(true,false).applyTo(propNameCmb);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Value:");
		
		propValueTxt = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(propValueTxt);
		
		setTitle("Edit Plate Property");
		setMessage("Add or modify a plate property below.");
		
		if (!plates.isEmpty()) {
			String[] propNames = plates.stream()
					.flatMap(p -> PlateService.getInstance().getPlateProperties(p).keySet().stream())
					.distinct()
					.sorted()
					.toArray(i -> new String[i]);
			propNameCmb.setItems(propNames);
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
			if (updatePlates()) close();
			return;
		}
		
		super.buttonPressed(buttonId);
	}
	
	private boolean updatePlates() {
		String propName = propNameCmb.getText();
		String propValue = propValueTxt.getText();
		if (propName.trim().isEmpty()) {
			MessageDialog.openError(getShell(), "Update failed", "The name cannot be empty.");
			return false;
		}
		try {
			for (Plate plate: plates) {
				PlateService.getInstance().setPlateProperty(plate, propName, propValue);
			}
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Update failed", "Failed to update plate property: " + e.getMessage());
			EclipseLog.error("Failed to update plate property", e, Activator.getDefault());
			return false;
		}
		return true;
	}
}