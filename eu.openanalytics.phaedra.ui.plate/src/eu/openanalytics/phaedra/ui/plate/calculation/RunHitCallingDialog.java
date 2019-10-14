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

import eu.openanalytics.phaedra.calculation.hitcall.HitCallService;
import eu.openanalytics.phaedra.calculation.hitcall.model.HitCallRule;
import eu.openanalytics.phaedra.calculation.hitcall.model.HitCallRuleset;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class RunHitCallingDialog extends TitleAreaDialog {

	private List<Plate> plates;
	private Map<HitCallRule, Double> customThresholds;
	
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
		HitCallRuleset[] rulesets = HitCallService.getInstance().getRulesetsForProtocolClass(pClass.getId()).values().stream()
				.sorted((r1, r2) -> r1.getFeature().getName().compareTo(r2.getFeature().getName()))
				.toArray(i -> new HitCallRuleset[i]);
		
		List<Text> texts = new ArrayList<>();
		for (HitCallRuleset rs: rulesets) {
			for (HitCallRule r: rs.getRules()) {
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
	
	public Map<HitCallRule, Double> getCustomThresholds() {
		return customThresholds;
	}
	
	private void updateCustomThreshold(HitCallRule rule, String text) {
		try {
			Double customTh = Double.valueOf(text);
			customThresholds.put(rule, customTh);
		} catch (NumberFormatException e) {}
	}
}