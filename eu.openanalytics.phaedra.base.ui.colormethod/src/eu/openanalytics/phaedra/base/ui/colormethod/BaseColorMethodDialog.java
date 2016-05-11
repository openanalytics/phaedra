package eu.openanalytics.phaedra.base.ui.colormethod;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public abstract class BaseColorMethodDialog extends TitleAreaDialog {

	private IColorMethod colorMethod;
	
	public BaseColorMethodDialog(Shell parentShell, IColorMethod cm) {
		super(parentShell);
		this.colorMethod = cm;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Color Method Settings");
	}
		
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite parentComposite = (Composite) super.createDialogArea(parent);

		Composite main = new Composite(parentComposite, SWT.NONE);
		
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(main);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(main);
		
		fillDialogArea(main);
		
		setTitle(colorMethod.getName());
		setMessage("You can configure the settings of the " 
				+ colorMethod.getName() + " color method below.");
		return main;
	}

	@Override
	protected void okPressed() {
		doApply();
		super.okPressed();
	}
	
	@Override
	protected void cancelPressed() {
		doCancel();
		super.cancelPressed();
	}
	
	protected IColorMethod getColorMethod() {
		return colorMethod;
	}
	
	protected abstract void fillDialogArea(Composite area);
	
	protected void doApply() {
		// Default: do nothing.
	}
	
	protected void doCancel() {
		// Default: do nothing.
	}
}
