package eu.openanalytics.phaedra.ui.curve.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.util.dialog.TitleAreaDataBindingDialog;
import eu.openanalytics.phaedra.model.curve.CurveFitException;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveFitSettings;
import eu.openanalytics.phaedra.model.curve.CurveUIFactory;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

/**
 * <p>
 * Dialog that shows the curve settings for a particular
 * compound/feature combination (if any), and allows the 
 * user to change these settings and apply them to the curve.
 * </p><p>
 * This effectively updates the curve settings in the database,
 * and overrides the protocol class curve settings <strong>until
 * the plate is recalculated.</strong>
 *  </p>
 */
public class EditCurveDialog extends TitleAreaDataBindingDialog {

	private CurveFitSettings customizedSettings;
	private Curve[] curves;
	
	public EditCurveDialog(Shell parentShell, Curve[] curves) {
		super(parentShell);
		if (curves == null || curves.length == 0) throw new IllegalArgumentException("No curve(s) selected.");
		this.curves = curves;
		
		CurveFitService curveFitService = CurveFitService.getInstance();
		CurveFitSettings curveSettings = curveFitService.getSettings(curves[0]);
		customizedSettings = (curveSettings != null) ? curveFitService.getWorkingCopy(curveSettings) : null;
		
		setDialogTitle("Edit Curve Fit Settings");
		String message = "You can change the settings for the selected curve(s) using the fields below.";
		if (curves.length > 1) {
			message += "\nPlease note that the below settings will be applied to all " + curves.length + " selected curves.";
			setDialogMessage(message, IMessageProvider.INFORMATION);
		} else {
			setDialogMessage(message);
		}
	}
	
	
	@Override
	protected Composite createDialogArea(Composite parent) {
		Composite area = super.createDialogAreaComposite(parent);
		
		Composite composite = CurveUIFactory.createFields(area, curves[0].getFeature(), customizedSettings, getDataBindingContext(), null);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
		
		return area;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, 2, "Apply", true);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == 2) {
			fitCustomCurve();
		} else {
			super.buttonPressed(buttonId);
		}
	}
	
	@Override
	protected void okPressed() {
		fitCustomCurve();
		super.okPressed();
	}
	
	private void fitCustomCurve() {
		if (curves.length == 0 || customizedSettings == null) return;
		for (Curve curve: curves) {
			try {
				CurveFitService.getInstance().updateCurveSettings(curve, customizedSettings);
				CurveFitService.getInstance().fitCurve(curve);
			} catch (CurveFitException e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						"Fit Failed", "Fit error: " + e.getMessage());
			}
		}
	}

}
