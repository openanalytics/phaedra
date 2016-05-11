package eu.openanalytics.phaedra.base.util.misc;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

public class EclipseLog {

	public static void debug(String msg, Class<?> clazz) {
		Logger log = Logger.getLogger(clazz);
		if (log.isDebugEnabled()) log.debug(msg);
	}
	
	public static void info(String msg, Plugin plugin) {
		log(new Status(IStatus.INFO, plugin.getBundle().getSymbolicName(), msg), plugin);
	}
	
	public static void warn(String msg, Plugin plugin) {
		log(new Status(IStatus.WARNING, plugin.getBundle().getSymbolicName(), msg), plugin);
	}
	
	public static void warn(String msg, Throwable cause, Plugin plugin) {
		log(new Status(IStatus.WARNING, plugin.getBundle().getSymbolicName(), msg, cause), plugin);
	}
	
	public static void error(String msg, Throwable cause, Plugin plugin) {
		log(new Status(IStatus.ERROR, plugin.getBundle().getSymbolicName(), msg, cause), plugin);
	}
	
	public static void log(IStatus status, Plugin plugin) {
		plugin.getLog().log(status);
	}
}
