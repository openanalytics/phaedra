package eu.openanalytics.phaedra.base.ui.colormethod;

import java.io.Serializable;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public abstract class BaseColorMethod implements IColorMethod, IExecutableExtension, Serializable {

	private static final long serialVersionUID = -2235298699205853899L;
	
	private String id;
	private String name;
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		this.id = config.getAttribute(IColorMethod.ATTR_ID);
		this.name = config.getAttribute(IColorMethod.ATTR_NAME);
	}
	
	@Override
	public RGB getColor(String v) {
		// By default, string values are not supported.
		return getColor(0.0d);
	}
	
	/*
	 * Helpers for accessing color method settings.
	 */
	
	public static String getSetting(String name, Map<String,String> settings, String defaultValue) {
		if (settings == null) return defaultValue;
		return settings.get(name);
	}
	
	public static RGB getSetting(String name, Map<String,String> settings, RGB defaultValue) {
		if (settings == null) return defaultValue;
		String value = settings.get(name);
		if (value == null) return defaultValue;
		return ColorUtils.parseRGBString(value);
	}
	
	public static double getSetting(String name, Map<String,String> settings, double defaultValue) {
		if (settings == null) return defaultValue;
		String value = settings.get(name);
		if (value == null || !NumberUtils.isDouble(value)) return defaultValue;
		return Double.parseDouble(value);
	}
	
	public static int getSetting(String name, Map<String,String> settings, int defaultValue) {
		if (settings == null) return defaultValue;
		String value = settings.get(name);
		if (value == null) return defaultValue;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
