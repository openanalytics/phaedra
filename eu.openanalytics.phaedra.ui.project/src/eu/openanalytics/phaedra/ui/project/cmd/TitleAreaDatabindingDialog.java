package eu.openanalytics.phaedra.ui.project.cmd;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.databinding.dialog.TitleAreaDialogSupport;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class TitleAreaDatabindingDialog extends TitleAreaDialog {

	private String title;

	private String defaultMessage;
	private int defaultMessageType;

	private DataBindingContext dbc;
	private boolean dbValidation = true;


	public TitleAreaDatabindingDialog(Shell parentShell) {
		super(parentShell);
	}


	protected void setDialogTitle(String title) {
		this.title = title;
	}
	
	protected void setDialogMessage(String message, int messageType) {
		defaultMessage = message;
		defaultMessageType = messageType;
	}
	
	protected void setDialogMessage(String message) {
		setDialogMessage(message, IMessageProvider.NONE);
	}
	
	protected void setValidationEnabled(boolean enabled) {
		dbValidation = enabled;
	}


	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (title != null) {
			newShell.setText(title);
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		
		if (title != null) {
			setTitle(title);
		}
		
		dbc = new DataBindingContext();
		initDatabinding(dbc);
		dbc.updateTargets();
		if (dbValidation) {
			TitleAreaDialogSupport.create(this, dbc);
		}
		setMessage(null, -1);
		
		return control;
	}

	@Override
	public void setMessage(String newMessage, int newType) {
		if (newMessage == null || newMessage.isEmpty()) {
			super.setMessage(defaultMessage, defaultMessageType);
		}
		else {
			super.setMessage(newMessage, newType);
		}
		if (newType >= 0) {
			getButton(IDialogConstants.OK_ID).setEnabled(newType != IMessageProvider.ERROR);
		}
	}
	
	@Override
	public void setErrorMessage(String newMessage) {
		setMessage(newMessage, IMessageProvider.ERROR);
	}


	protected void initDatabinding(DataBindingContext dbc) {
	}

}
