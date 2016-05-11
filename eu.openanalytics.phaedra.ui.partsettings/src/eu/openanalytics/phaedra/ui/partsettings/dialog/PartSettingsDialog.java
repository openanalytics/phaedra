package eu.openanalytics.phaedra.ui.partsettings.dialog;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.ui.partsettings.utils.PartSettingsUtils;
import eu.openanalytics.phaedra.ui.partsettings.vo.PartSettings;

public class PartSettingsDialog extends TitleAreaDialog {

	private PartSettings partSettings;
	private String title;
	private IInputValidator validator;

	private Text name;
	private Button isGlobal;
	private Button isTemplate;

	public PartSettingsDialog(Shell parentShell, String title, PartSettings partSettings) {
		super(parentShell);
		this.partSettings = partSettings;
		this.title = title;
		this.validator = PartSettingsUtils.NAME_INPUT_VALIDATOR;
	}

	@Override
	public void create() {
		super.create();
		getShell().setText(title);
		setTitle(title);
		setMessage("Please specify a name to identify the settings.", IMessageProvider.NONE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		Composite container = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Name: ");
		lbl.setToolTipText("The name that will be used to identify these settings.");

		name = new Text(container, SWT.SINGLE | SWT.BORDER);
		name.setText(partSettings.getName());
		name.setToolTipText(lbl.getToolTipText());
		name.addListener(SWT.Modify, e -> validateInput());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(name);

		lbl = new Label(container, SWT.NONE);
		lbl.setText("Global: ");
		lbl.setToolTipText("When checked, these settings will not be protocol specific.");

		isGlobal = new Button(container, SWT.CHECK);
		isGlobal.setSelection(partSettings.isGlobal());
		isGlobal.setToolTipText(lbl.getToolTipText());

		boolean isAdmin = SecurityService.getInstance().isGlobalAdmin();

		lbl = new Label(container, SWT.NONE);
		lbl.setText("Template: ");
		lbl.setToolTipText("When checked, these settings will be available as a template to all Phaedra users.");
		lbl.setEnabled(isAdmin);
		lbl.setVisible(isAdmin);

		isTemplate = new Button(container, SWT.CHECK);
		isTemplate.setSelection(partSettings.isTemplate());
		isTemplate.setToolTipText(lbl.getToolTipText());
		isTemplate.setEnabled(isAdmin);
		isTemplate.setVisible(isAdmin);

		return area;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void okPressed() {
		partSettings.setName(name.getText());
		partSettings.setGlobal(isGlobal.getSelection());
		partSettings.setTemplate(isTemplate.getSelection());
		super.okPressed();
	}

	private void validateInput() {
        String errorMessage = validator.isValid(name.getText());
        setErrorMessage(errorMessage);
    }

}
