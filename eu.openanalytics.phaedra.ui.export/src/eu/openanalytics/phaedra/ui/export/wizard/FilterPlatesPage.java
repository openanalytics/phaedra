package eu.openanalytics.phaedra.ui.export.wizard;

import java.util.Calendar;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.export.core.IFilterPlatesSettings;
import eu.openanalytics.phaedra.ui.export.widget.EuroCalendarCombo;

public class FilterPlatesPage extends BaseExportWizardPage {
	
	private IFilterPlatesSettings settings;
	
	private Button filterOnValidationChk;
	private Text validationUserTxt;
	private EuroCalendarCombo validationDateFromCombo;
	private EuroCalendarCombo validationDateToCombo;
	
	private Button filterOnApprovalChk;
	private Text approvalUserTxt;
	private EuroCalendarCombo approvalDateFromCombo;
	private EuroCalendarCombo approvalDateToCombo;
	
	private Button invalidPlatesChk;
	private Button disapprovedPlatesChk;
	
	public FilterPlatesPage(IFilterPlatesSettings settings, int stepNum, int stepTotal) {
		super("Filter Plates");
		setDescription(String.format("Step %1$s/%2$s: Select the plates to export.", stepNum, stepTotal));
		
		this.settings = settings;
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10,10).numColumns(2).applyTo(container);
		setControl(container);

		filterOnValidationChk = new Button(container, SWT.CHECK);
		filterOnValidationChk.setText("Filter on Validation");
		GridDataFactory.fillDefaults().span(2,1).applyTo(filterOnValidationChk);
		
		filterOnValidationChk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = filterOnValidationChk.getSelection();
				validationUserTxt.setEnabled(checked);
				validationDateFromCombo.setEnabled(checked);
				validationDateToCombo.setEnabled(checked);
			}
		});
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Validated by User:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		validationUserTxt = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().applyTo(validationUserTxt);
		validationUserTxt.setEnabled(false);
		
		label = new Label(container, SWT.NONE);
		label.setText("Validation Date:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		Composite subContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(subContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(subContainer);
		
		label = new Label(subContainer, SWT.NONE);
		label.setText("From:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		validationDateFromCombo = new EuroCalendarCombo(subContainer, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(validationDateFromCombo);
		validationDateFromCombo.setEnabled(false);
		
		label = new Label(subContainer, SWT.NONE);
		label.setText("To:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		validationDateToCombo = new EuroCalendarCombo(subContainer, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(validationDateToCombo);
		validationDateToCombo.setEnabled(false);
		
		label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).applyTo(label);
		
		filterOnApprovalChk = new Button(container, SWT.CHECK);
		filterOnApprovalChk.setText("Filter on Approval");
		GridDataFactory.fillDefaults().span(2,1).applyTo(filterOnApprovalChk);
		
		filterOnApprovalChk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean checked = filterOnApprovalChk.getSelection();
				approvalUserTxt.setEnabled(checked);
				approvalDateFromCombo.setEnabled(checked);
				approvalDateToCombo.setEnabled(checked);
			}
		});
		
		label = new Label(container, SWT.NONE);
		label.setText("Approved by User:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		approvalUserTxt = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().applyTo(approvalUserTxt);
		approvalUserTxt.setEnabled(false);
		
		label = new Label(container, SWT.NONE);
		label.setText("Approval Date:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		subContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(subContainer);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(subContainer);
		
		label = new Label(subContainer, SWT.NONE);
		label.setText("From:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		approvalDateFromCombo = new EuroCalendarCombo(subContainer, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(approvalDateFromCombo);
		approvalDateFromCombo.setEnabled(false);
		
		label = new Label(subContainer, SWT.NONE);
		label.setText("To:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		approvalDateToCombo = new EuroCalendarCombo(subContainer, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(approvalDateToCombo);
		approvalDateToCombo.setEnabled(false);
		
		label = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).applyTo(label);
		
		invalidPlatesChk = new Button(container, SWT.CHECK);
		invalidPlatesChk.setText("Include invalidated plates");
		GridDataFactory.fillDefaults().span(2,1).applyTo(invalidPlatesChk);
		
		disapprovedPlatesChk = new Button(container, SWT.CHECK);
		disapprovedPlatesChk.setText("Include disapproved plates");
		GridDataFactory.fillDefaults().span(2,1).applyTo(disapprovedPlatesChk);
		
		loadDialogSettings();
	}
	
	@Override
	public void collectSettings() {
		Calendar date;
		
		settings.setFilterValidation(filterOnValidationChk.getSelection());
		settings.setValidationUser(validationUserTxt.getText());
		date = validationDateFromCombo.getDate();
		settings.setValidationDateFrom((date != null) ? date.getTime() : null);
		date = validationDateToCombo.getDate();
		settings.setValidationDateTo((date != null) ? date.getTime() : null);
		
		settings.setFilterApproval(filterOnApprovalChk.getSelection());
		settings.setApprovalUser(approvalUserTxt.getText());
		date = approvalDateFromCombo.getDate();
		settings.setApprovalDateFrom((date != null) ? date.getTime() : null);
		date = approvalDateToCombo.getDate();
		settings.setApprovalDateTo((date != null) ? date.getTime() : null);
		
		settings.setIncludeInvalidatedPlates(invalidPlatesChk.getSelection());
		settings.setIncludeDisapprovedPlates(disapprovedPlatesChk.getSelection());
	}
	
	private void loadDialogSettings() {
		IDialogSettings dialogSettings = getDialogSettings();
		
		invalidPlatesChk.setSelection(dialogSettings.getBoolean("invalidPlatesChk"));
		disapprovedPlatesChk.setSelection(dialogSettings.getBoolean("disapprovedPlatesChk"));
	}
	
	@Override
	public void saveDialogSettings() {
		IDialogSettings dialogSettings = getDialogSettings();
		
		dialogSettings.put("invalidPlatesChk", settings.getIncludeInvalidatedPlates());
		dialogSettings.put("disapprovedPlatesChk", settings.getIncludeDisapprovedPlates());
	}
	
}
