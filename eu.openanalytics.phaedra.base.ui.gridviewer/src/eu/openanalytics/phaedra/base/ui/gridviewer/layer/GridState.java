package eu.openanalytics.phaedra.base.ui.gridviewer.layer;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * This class stores and retrieves state information about plate grid layers,
 * for the duration of the session.
 * 
 * If protocolId is -1, the requested setting is independent of protocol
 * selection.
 * 
 * E.g. layers that are enabled on a Quick Heatmap, will be enabled also the
 * next time a Quick Heatmap is opened.
 */
public class GridState {
	
	public final static String DEFAULT_ENABLED = "DEFAULT_ENABLED";
	public final static long ALL_PROTOCOLS = -1;
	
	private final static String KEY_PATTERN = "{0}#{1}#{2}";
	
	private static Map<String, Object> settingMap = new HashMap<String, Object>();

	public static void saveValue(long protocolId, String layerId, String setting, Object value) {
		settingMap.put(getKey(protocolId, layerId, setting), value);
	}

	public static Object getValue(long protocolId, String layerId, String setting) {
		return settingMap.get(getKey(protocolId, layerId, setting));
	}

	public static void removeValue(long protocolId, String layerId, String setting) {
		settingMap.remove(getKey(protocolId, layerId, setting));
	}

	public static void clear() {
		settingMap.clear();
	}
	
	private static String getKey(long protocolId, String layerId, String setting) {
		return MessageFormat.format(KEY_PATTERN, protocolId, layerId, setting);
	}

	public static Boolean getBooleanValue(long protocolId, String layerId, String setting) {
		return (Boolean) getValue(protocolId, layerId, setting);
	}
	
	public static String getStringValue(long protocolId, String layerId, String setting) {
		return (String) getValue(protocolId, layerId, setting);
	}
	
	public static Double getDoubleValue(long protocolId, String layerId, String setting) {
		return (Double) getValue(protocolId, layerId, setting);
	}
	
	public static Integer getIntegerValue(long protocolId, String layerId, String setting) {
		return (Integer) getValue(protocolId, layerId, setting);
	}
}
