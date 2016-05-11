package eu.openanalytics.phaedra.link.platedef.hook;

import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;

public class LinkPlateDefHookArguments implements IHookArguments {

	public LinkPlateDefHookArguments(String plateSource, PlateLinkSettings settings) {
		this.plateSource = plateSource;
		this.settings = settings;
	}
	
	public String plateSource;
	public PlateLinkSettings settings;
	
}
