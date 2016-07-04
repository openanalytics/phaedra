package eu.openanalytics.phaedra.ui.cellprofiler.widget;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class EditPatternDialog extends TitleAreaDialog {

	private PatternTester patternTester;
	private PatternConfig patternConfig;
	
	public EditPatternDialog(Shell parentShell, PatternConfig patternConfig) {
		super(parentShell);
		this.patternConfig = patternConfig;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Pattern");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		patternTester = new PatternTester();
		patternTester.createComposite(main);
		patternTester.loadConfig(patternConfig, this::setErrorMessage);
		
		setTitle("Edit Pattern");
		setMessage("Adjust the pattern for the image files of this channel.");
		return main;
	}
}
