package eu.openanalytics.phaedra.ui.protocol.template.simple;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;

public class SimpleProtocolWizardState implements IWizardState {

	public Map<String, Object> parameters = new HashMap<>();

	public SimpleProtocolWizardState() {
		parameters.put("protocol.name", "New Protocol");
		parameters.put("protocol.team", "Global");
		parameters.put("is.plate.folder", "false");
	}
}
