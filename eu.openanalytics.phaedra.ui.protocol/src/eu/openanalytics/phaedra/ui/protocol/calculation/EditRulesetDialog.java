package eu.openanalytics.phaedra.ui.protocol.calculation;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.calculation.formula.model.FormulaRuleset;

public class EditRulesetDialog extends TitleAreaDialog {

	private RulesetEditor rulesetEditor;
	
	private FormulaRuleset ruleset;
	
	public EditRulesetDialog(Shell parentShell, FormulaRuleset ruleset) {
		super(parentShell);
		this.ruleset = ruleset;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Formula Ruleset");
		newShell.setSize(600, 500);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(1).applyTo(main);
		
		rulesetEditor = new RulesetEditor(main, SWT.NONE);
		rulesetEditor.setInput(ruleset);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(rulesetEditor);
		
		setTitle("Edit Formula Ruleset");
		setMessage("Adjust the rules and properties of the formula ruleset below.");
		
		return main;
	}
}
