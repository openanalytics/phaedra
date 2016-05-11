package eu.openanalytics.phaedra.datacapture.module;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;

public abstract class AbstractModule implements IModule {
	
	private ModuleConfig config;

	@Override
	public String getName() {
		if (config == null) return null;
		return config.getName();
	}

	@Override
	public String getId() {
		return config.getId();
	}

	@Override
	public ModuleConfig getConfig() {
		return config;
	}
	
	@Override
	public void configure(ModuleConfig cfg) throws DataCaptureException {
		this.config = cfg;
	}
	
	@Override
	public void postCapture(DataCaptureContext context, IProgressMonitor monitor) {
		// Default behaviour: do nothing.
	}
	
	@Override
	public int getWeight() {
		return 1;
	}
}
