package eu.openanalytics.phaedra.ui.protocol.calculation;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.calculation.hitcall.model.HitCallRuleset;

// Java.type("eu.openanalytics.phaedra.ui.protocol.calculation.EditHitCallRulesetDialog").openNew(API.get("HitCallService").getRuleset(3))
//TODO workingCopy
public class EditHitCallRulesetDialog extends TitleAreaDialog {

	private HitCallRulesetEditor rulesetEditor;
	
	private HitCallRuleset ruleset;
	
	public EditHitCallRulesetDialog(Shell parentShell, HitCallRuleset ruleset) {
		super(parentShell);
		this.ruleset = ruleset;
	}

	public static void openNew(HitCallRuleset ruleset) {
		EditHitCallRulesetDialog dialog = new EditHitCallRulesetDialog(Display.getDefault().getActiveShell(), ruleset);
		dialog.open();
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Hit Calling Ruleset");
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
		
		rulesetEditor = new HitCallRulesetEditor(main, SWT.NONE);
		rulesetEditor.setInput(ruleset);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(rulesetEditor);
		
		setTitle("Edit Hit Calling Ruleset");
		setMessage("Adjust the rules and properties of the hit calling ruleset below.");
		
		return main;
	}
}
