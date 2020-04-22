package eu.openanalytics.phaedra.base.ui.util.dialog;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.databinding.dialog.TitleAreaDialogSupport;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;


public class TitleAreaDataBindingDialog extends TitleAreaDialog {

	private String title;

	private String defaultMessage;
	private int defaultMessageType;

	private DataBindingContext dbc;
	private boolean dbValidation = true;


	public TitleAreaDataBindingDialog(Shell parentShell) {
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
		dbc = new DataBindingContext();
		
		Control control = super.createContents(parent);
		
		if (title != null) {
			setTitle(title);
		}
		
		initDataBinding(dbc);
		updateTargets();
		if (dbValidation) {
			updateModels();
			TitleAreaDialogSupport.create(this, dbc);
		}
		getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				dbc.dispose();
				dbc = null;
			}
		});
		setMessage(null, -1);
		
		return control;
	}
	
	protected Composite createDialogAreaComposite(Composite parent) {
		Composite area = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(area);
		GridLayoutFactory.fillDefaults()
				.margins(convertVerticalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN), convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN))
				.applyTo(area);
		return area;
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
			applyValidationResult(newType != IMessageProvider.ERROR);
		}
	}
	
	@Override
	public void setErrorMessage(String newMessage) {
		setMessage(newMessage, IMessageProvider.ERROR);
	}
	
	protected void applyValidationResult(final boolean isValid) {
		getButton(IDialogConstants.OK_ID).setEnabled(isValid);
	}
	
	
	protected DataBindingContext getDataBindingContext() {
		return this.dbc;
	}
	
	protected void updateModels() {
		this.dbc.updateModels();
	}
	
	protected void updateTargets() {
		this.dbc.updateTargets();
	}
	
	protected void initDataBinding(DataBindingContext dbc) {
	}

}
