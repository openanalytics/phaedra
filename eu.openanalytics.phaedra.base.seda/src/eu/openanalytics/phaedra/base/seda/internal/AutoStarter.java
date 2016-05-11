package eu.openanalytics.phaedra.base.seda.internal;

import org.eclipse.ui.IStartup;

import eu.openanalytics.phaedra.base.seda.Activator;

public class AutoStarter implements IStartup {
	
	@Override
	public void earlyStartup() {
		// Normally, earlyStartup() being called already activated the plugin,
		// but call the Activator explicitly just to make sure.
		Activator.getDefault();
	}
}
