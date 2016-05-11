package eu.openanalytics.phaedra.link.platedef.source;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.link.platedef.Activator;

public class SourceRegistry {

	private Map<String, IPlateDefinitionSource> sources;
	
	public SourceRegistry() {
		loadSources();
	}
	
	public String[] getIds() {
		return sources.keySet().toArray(new String[sources.size()]);
	}
	
	public IPlateDefinitionSource getSource (String id) {
		return sources.get(id);
	}
	
	private void loadSources() {
		sources = new HashMap<String, IPlateDefinitionSource>();
		
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IPlateDefinitionSource.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			String sourceId = el.getAttribute("id");
			try {
				Object o = el.createExecutableExtension("class");
				IPlateDefinitionSource source = (IPlateDefinitionSource)o;
				if (source.isAvailable()) sources.put(sourceId, source);
				else EclipseLog.warn("Skipping unavailable plate definition source: '" + sourceId + "'", Activator.getDefault());
			} catch (CoreException e) {
				// Invalid extension, ignore.
			}
		}
	}
}
