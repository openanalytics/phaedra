package eu.openanalytics.phaedra.calculation.norm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class NormalizationRegistry {

	private Map<String,INormalizer> normalizers;
	
	public NormalizationRegistry() {
		normalizers = new HashMap<String, INormalizer>();
		loadNormalizers();
	}
	
	public String[] getNormalizationIds() {
		String[] ids = normalizers.keySet().toArray(new String[normalizers.size()]);
		Arrays.sort(ids);
		return ids;
	}
	
	public INormalizer getNormalizer(String id) {
		return normalizers.get(id);
	}
	
	private void loadNormalizers() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(INormalizer.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(INormalizer.ATTR_CLASS);
				if (o instanceof INormalizer) {
					INormalizer normalizer = (INormalizer)o;
					String id = normalizer.getId();
					normalizers.put(id, normalizer);
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
	}
}
