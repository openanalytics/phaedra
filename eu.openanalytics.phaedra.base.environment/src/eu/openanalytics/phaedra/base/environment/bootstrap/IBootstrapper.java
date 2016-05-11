package eu.openanalytics.phaedra.base.environment.bootstrap;

import eu.openanalytics.phaedra.base.environment.Activator;
import eu.openanalytics.phaedra.base.environment.IEnvironment;

public interface IBootstrapper {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".bootstrapper";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_SEQUENCE = "sequence";
	
	public void bootstrap(IEnvironment env) throws BootstrapException;
}
