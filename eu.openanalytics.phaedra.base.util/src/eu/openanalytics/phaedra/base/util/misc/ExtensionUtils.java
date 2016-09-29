package eu.openanalytics.phaedra.base.util.misc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.util.Activator;

public class ExtensionUtils {

	public static <T> List<T> createInstanceList(String extPtId, String classAttribute, Class<T> clazz) {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(extPtId);
		return Arrays.stream(elements).map(e -> createInstance(e, classAttribute, clazz)).collect(Collectors.toList());
	}
	
	public static <T> Map<String, T> createInstanceMap(String extPtId, String idAttribute, String classAttribute, Class<T> clazz) {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(extPtId);
		Map<String, T> map = new HashMap<>();
		Arrays.stream(elements).forEach(e -> map.put(e.getAttribute(idAttribute), createInstance(e, classAttribute, clazz)));
		return map;
	}
	
	public static <T> T createInstance(String extPtId, String idAttribute, String idToMatch, String classAttribute, Class<T> clazz) {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(extPtId);
		return Arrays.stream(elements)
				.filter(e -> e.getAttribute(idAttribute).equals(idToMatch)).map(e -> createInstance(e, classAttribute, clazz))
				.findAny().orElse(null);	
	}
	
	public static <T> T createInstance(IConfigurationElement element, String classAttribute, Class<T> clazz) {
		try {
			return clazz.cast(element.createExecutableExtension(classAttribute));
		} catch (CoreException e) {
			EclipseLog.warn("Invalid extension class: " + classAttribute, Activator.getDefault());
			return null;
		}
	}
}
