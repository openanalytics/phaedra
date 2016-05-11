package eu.openanalytics.phaedra.base.ui.charting.v2.chart.gates;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.runtime.Status;
import org.flowcyt.facejava.gating.gates.Gate;
import org.flowcyt.facejava.gating.gates.GateSet;
import org.flowcyt.facejava.gating.xmlio.GatingMLFileReader;

import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.ui.charting.Activator;

public class GateFactory {

	private static final String GATE_ = "GATE_";

	/**
	 * Create a GateSet containing all the gates that from the given XML String array.
	 *
	 * If the given key is present in the cache, the cache value will be used.
	 *
	 * @param keyPart The key which will be used for caching (e.g. Well).
	 * @param gateXMLs Array of GateML XML Strings.
	 * @return A GateSet containing all the gates.
	 */
	public static GateSet createGateSetFromGateXML(Object keyPart, String[] gateXMLs) {
		ICache cache = CacheService.getInstance().getDefaultCache();

		String key = GATE_ + keyPart.hashCode();
		Object o = cache.get(key);
		if (o == null) {
			GateSet gateSet = new GateSet();
			for (String gateContent : gateXMLs) {
				if (gateContent == null) continue;
				try {
					GatingMLFileReader gatingReader = new GatingMLFileReader(new ByteArrayInputStream(gateContent.getBytes("UTF-8")));
					for (Gate gate : gatingReader.read()) {
						gateSet.add(gate);
					}
				} catch (UnsupportedEncodingException e) {
					Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed to load gate", e));
				}
			}
			o = cache.put(key, gateSet);
		}

		return (GateSet) o;
	}

}