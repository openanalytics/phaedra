package eu.openanalytics.phaedra.base.ui.colormethod.lookup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.BaseColorMethodDialog;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.ui.colormethod.LegendDrawer;

public class LookupColorMethod extends BaseColorMethod {

	private static final long serialVersionUID = -6746762510272996808L;

	public final static String SETTING_RULESET = "ruleset";

	private List<LookupRule> rules;

	@Override
	public void configure(Map<String, String> settings) {
		rules = new ArrayList<LookupRule>();
		if (settings == null) return;

		String ruleSet = settings.get(SETTING_RULESET);
		if (ruleSet != null && !ruleSet.isEmpty()) {
			try {
				InputStream in = new ByteArrayInputStream(ruleSet.getBytes());
				rules = new LookupRuleParser().parse(in);
			} catch (IOException e) {}
		}
	}

	@Override
	public void getConfiguration(Map<String, String> settings) {
		String ruleSet = new LookupRuleParser().write(rules);
		settings.put(SETTING_RULESET, ruleSet);
	}

	@Override
	public void initialize(IColorMethodData dataset) {
		// No initialization required.
	}

	@Override
	public RGB getColor(double v) {
		for (LookupRule rule: rules) {
			if (rule.matches(v)) return rule.getColor();
		}
		return new RGB(0,0,0);
	}

	@Override
	public Image getLegend(int width, int height, int orientation, boolean labels, double[] highlightValues) {
		return getLegend(width, height, orientation, labels, highlightValues, false);
	}

	@Override
	public Image getLegend(int width, int height, int orientation, boolean labels, double[] highlightValues, boolean isWhiteBackground) {
		return new LegendDrawer(orientation).getLookupLegend(
				rules, highlightValues, width, height, isWhiteBackground);
	}

	@Override
	public BaseColorMethodDialog createDialog(Shell shell) {
		return new LookupColorMethodDialog(shell, this);
	}

	public List<LookupRule> getRules() {
		return rules;
	}

	public static String getConditionLabel(String condition) {
		String lbl = "else";
		if (condition.equals("lt")) lbl = "<";
		if (condition.equals("gt")) lbl = ">";
		if (condition.equals("le")) lbl = "<=";
		if (condition.equals("ge")) lbl = ">=";
		if (condition.equals("eq")) lbl = "=";
		if (condition.equals("ne")) lbl = "<>";
		return lbl;
	}
}
