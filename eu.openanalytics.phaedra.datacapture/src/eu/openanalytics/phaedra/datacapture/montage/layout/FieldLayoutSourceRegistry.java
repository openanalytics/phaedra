package eu.openanalytics.phaedra.datacapture.montage.layout;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.montage.MontageConfig;

public class FieldLayoutSourceRegistry {

	private Map<String, IFieldLayoutSource> sources;
	
	private final static String DEFAULT_SOURCE = "literal";
	
	private static FieldLayoutSourceRegistry instance;
	
	private FieldLayoutSourceRegistry() {
		// Hidden constructor
		loadSources();
	}
	
	public static synchronized FieldLayoutSourceRegistry getInstance() {
		if (instance == null) instance = new FieldLayoutSourceRegistry();
		return instance;
	}
	
	public IFieldLayoutSource getSource(String id) {
		if (id == null || id.isEmpty()) id = DEFAULT_SOURCE;
		return sources.get(id);
	}
	
	public FieldLayout getLayout(PlateReading reading, int fieldCount, MontageConfig montageConfig, DataCaptureContext context) {
		FieldLayout layout = null;
		IFieldLayoutSource source = getSource(montageConfig.layoutSource);
		if (source != null) {
			layout = source.getLayout(reading, fieldCount, montageConfig, context);
		}
		return layout;
	}
	
	private void loadSources() {
		sources = new HashMap<>();
		
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IFieldLayoutSource.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IFieldLayoutSource.ATTR_CLASS);
				if (o instanceof IFieldLayoutSource) {
					IFieldLayoutSource source = (IFieldLayoutSource)o;
					String id = el.getAttribute(IFieldLayoutSource.ATTR_ID);
					sources.put(id, source);
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
	}
}
