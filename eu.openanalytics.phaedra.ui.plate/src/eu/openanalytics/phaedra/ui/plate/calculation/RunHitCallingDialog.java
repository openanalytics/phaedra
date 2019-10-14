package eu.openanalytics.phaedra.ui.plate.calculation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRule;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRuleset;
import eu.openanalytics.phaedra.calculation.formula.model.RulesetType;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class RunHitCallingDialog extends TitleAreaDialog {

	private List<Plate> plates;
	private Map<FormulaRule, Double> customThresholds;
	
	public RunHitCallingDialog(Shell parentShell, List<Plate> plates) {
		super(parentShell);
		this.plates = plates;
		this.customThresholds = new HashMap<>();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Hit Calling");
		newShell.setSize(600, 300);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(main);
		
		ProtocolClass pClass = ProtocolUtils.getProtocolClass(plates.get(0));
		FormulaRuleset[] rulesets = FormulaService.getInstance().getRulesetsForProtocolClass(pClass.getId(), RulesetType.HitCalling.getCode())
				.values().stream()
				.sorted((r1, r2) -> r1.getFeature().getName().compareTo(r2.getFeature().getName()))
				.toArray(i -> new FormulaRuleset[i]);
		
		List<Text> texts = new ArrayList<>();
		for (FormulaRuleset rs: rulesets) {
			for (FormulaRule r: rs.getRules()) {
				customThresholds.put(r, r.getThreshold());
				
				new Label(main, SWT.NONE).setText(String.format("%s, %s:", rs.getFeature().getName(), r.getName()));
				Text text = new Text(main, SWT.BORDER);
				text.setText(String.valueOf(r.getThreshold()));
				text.addModifyListener(e -> updateCustomThreshold(r, text.getText()));
				texts.add(text);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
			}
		}

		setTitle("Hit Calling");
		setMessage(String.format("Adjust any thresholds with custom values."
				+ "\nThen, select Ok to trigger re-calculation of the %d selected plate(s).", plates.size()));
		
		return main;
	}
	
	public Map<FormulaRule, Double> getCustomThresholds() {
		return customThresholds;
	}
	
	private void updateCustomThreshold(FormulaRule rule, String text) {
		try {
			Double customTh = Double.valueOf(text);
			customThresholds.put(rule, customTh);
		} catch (NumberFormatException e) {}
	}
}