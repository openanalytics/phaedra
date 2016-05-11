package eu.openanalytics.phaedra.ui.curve.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveKind;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveMethod;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveModel;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveType;
import eu.openanalytics.phaedra.model.curve.fit.CurveFitException;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.curve.vo.OSBCurve;

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
public class EditCurveDialog extends TitleAreaDialog {

	private Combo methodCmb;
	private Combo modelCmb;
	private Combo typeCmb;
	
	private Text lbTxt;
	private Text ubTxt;
	
	private Text thresholdTxt;
	
	private Curve[] curves;
	
	public EditCurveDialog(Shell parentShell, Curve[] curves) {
		super(parentShell);
		if (curves == null) throw new IllegalArgumentException("No curve(s) selected.");
		this.curves = curves;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Curve Settings");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		
		Composite area = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(area);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(15,15).applyTo(area);
		
		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Method:");
		
		methodCmb = new Combo(area, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(methodCmb);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Model:");
		
		modelCmb = new Combo(area, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(modelCmb);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Type:");
		
		typeCmb = new Combo(area, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(typeCmb);
		
		lbl = new Label(area, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).applyTo(lbl);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Manual LB:");
		
		lbTxt = new Text(area, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(lbTxt);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Manual UB:");
		
		ubTxt = new Text(area, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(ubTxt);
		
		lbl = new Label(area, SWT.NONE);
		lbl.setText("Threshold:");
		
		thresholdTxt = new Text(area, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(thresholdTxt);
		
		String message = "You can change the settings for the selected curve(s) using the fields below.";
		if (curves.length > 1) {
			message += "\nPlease note that the below settings will be applied to all " + curves.length + " selected curves.";
		}
		setMessage(message);
		setTitle("Edit Curve Fit Settings");
			
		initFields();
		
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
		//TODO This will fail if curves with different kinds are selected.
		
		for (Curve curve: curves) {
			CurveSettings customSettings = new CurveSettings();
			customSettings.setKind(curve.getFeature().getCurveSettings().get(CurveSettings.KIND));
			customSettings.setMethod(methodCmb.getText());
			customSettings.setModel(modelCmb.getText());
			customSettings.setType(typeCmb.getText());
			if (curve instanceof OSBCurve) {
				String lb = lbTxt.getText();
				if (lb != null && NumberUtils.isDouble(lb)) customSettings.setLb(Double.parseDouble(lb));
				else customSettings.setLb(Double.NaN);
				String ub = ubTxt.getText();
				if (ub != null && NumberUtils.isDouble(ub)) customSettings.setUb(Double.parseDouble(ub));
				else customSettings.setUb(Double.NaN);
			} else {
				String th = thresholdTxt.getText();
				if (th != null && NumberUtils.isDouble(th)) customSettings.setThreshold(Double.parseDouble(th));
				else customSettings.setThreshold(Double.NaN);
			}
			
			try {
				CurveService.getInstance().updateCurveSettings(curve, customSettings);
				CurveService.getInstance().fitCurve(curve);
			} catch (CurveFitException e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						"Fit Failed", "Fit error: " + e.getMessage());
			}
		}
	}
	
	private void initFields() {
		if (curves.length == 0) return;
		
		// Get the settings (default or custom) that were used to generate this curve.
		// Note that these may be different from curve.getCurveSettings(), e.g. if LIN or CENS fallback was used instead of OLS.
		CurveSettings settings = CurveService.getInstance().getCurveSettings(curves[0]);
		String kind = settings.getKind();
		String method = settings.getMethod();
		String model = settings.getModel();
		String type = settings.getType();
		double lb = settings.getLb();
		double ub = settings.getUb();
		double th = settings.getThreshold();
		
		CurveKind curveKind = CurveKind.valueOf(kind);
		CurveMethod[] methods = CurveService.getInstance().getCurveMethods(curveKind);
		CurveModel[] models = CurveService.getInstance().getCurveModels(curveKind);
		CurveType[] types = CurveService.getInstance().getCurveTypes();
		
		methodCmb.setItems(StringUtils.getEnumNames(methods));
		select(methodCmb, method);
		modelCmb.setItems(StringUtils.getEnumNames(models));
		select(modelCmb, model);
		typeCmb.setItems(StringUtils.getEnumNames(types));
		select(typeCmb, type);
		
		lbTxt.setText(Double.isNaN(lb)?"":""+lb);
		lbTxt.setEnabled(kind.equals(CurveKind.OSB.toString()));
		ubTxt.setText(Double.isNaN(ub)?"":""+ub);
		ubTxt.setEnabled(kind.equals(CurveKind.OSB.toString()));
		thresholdTxt.setText(Double.isNaN(th)?"":""+th);
		thresholdTxt.setEnabled(kind.equals(CurveKind.PLAC.toString()));
	}
	
	private void select(Combo cmb, String value) {
		if (value != null) {
			for (int i=0; i<cmb.getItemCount(); i++) {
				if (cmb.getItem(i).equals(value)) {
					cmb.select(i);
					break;
				}
			}
		}
	}
}
