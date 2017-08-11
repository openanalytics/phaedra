package eu.openanalytics.phaedra.base.util.misc;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

public class EclipseLog {

	public static void debug(String msg, Class<?> clazz) {
		Logger log = Logger.getLogger(clazz);
		if (log.isDebugEnabled()) log.debug(msg);
	}
	
	public static void info(String msg, Plugin plugin) {
		info(msg, plugin.getBundle());
	}
	
	public static void info(String msg, String bundleId) {
		info(msg, Platform.getBundle(bundleId));
	}
	
	public static void info(String msg, Bundle bundle) {
		log(new Status(IStatus.INFO, bundle.getSymbolicName(), msg), bundle);
	}
	
	public static void warn(String msg, Plugin plugin) {
		warn(msg, plugin.getBundle());
	}
	
	public static void warn(String msg, String bundleId) {
		warn(msg, Platform.getBundle(bundleId));
	}
	
	public static void warn(String msg, Bundle bundle) {
		log(new Status(IStatus.WARNING, bundle.getSymbolicName(), msg), bundle);
	}
	
	public static void warn(String msg, Throwable cause, Plugin plugin) {
		warn(msg, cause, plugin.getBundle());
	}
	
	public static void warn(String msg, Throwable cause, String bundleId) {
		warn(msg, cause, Platform.getBundle(bundleId));
	}
	
	public static void warn(String msg, Throwable cause, Bundle bundle) {
		log(new Status(IStatus.WARNING, bundle.getSymbolicName(), msg, cause), bundle);
	}
	
	public static void error(String msg, Throwable cause, Plugin plugin) {
		error(msg, cause, plugin.getBundle());
	}
	
	public static void error(String msg, Throwable cause, String bundleId) {
		error(msg, cause, Platform.getBundle(bundleId));
	}
	
	public static void error(String msg, Throwable cause, Bundle bundle) {
		log(new Status(IStatus.ERROR, bundle.getSymbolicName(), msg, cause), bundle);
	}
	
	public static void log(IStatus status, Plugin plugin) {
		log(status, plugin.getBundle());
	}
	
	public static void log(IStatus status, Bundle bundle) {
		Platform.getLog(bundle).log(status);
	}
}
