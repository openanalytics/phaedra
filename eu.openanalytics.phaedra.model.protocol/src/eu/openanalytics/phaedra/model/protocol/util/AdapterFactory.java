package eu.openanalytics.phaedra.model.protocol.util;

import org.eclipse.core.runtime.IAdapterFactory;

import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class AdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof SubWellFeature) {
			SubWellFeature swf = (SubWellFeature) adaptableObject;
			if (adapterType == ProtocolClass.class) return adapterType.cast(swf.getProtocolClass());
		}
		if (adaptableObject instanceof Feature) {
			Feature f = (Feature) adaptableObject;
			if (adapterType == ProtocolClass.class) return adapterType.cast(f.getProtocolClass());
		}
		if (adaptableObject instanceof FeatureClass) {
			FeatureClass fc = (FeatureClass) adaptableObject;
			if (adapterType == ProtocolClass.class) return adapterType.cast(fc.getSubWellFeature().getProtocolClass());
			else if (adapterType == SubWellFeature.class) return adapterType.cast(fc.getSubWellFeature());
		}
		if (adaptableObject instanceof Protocol) {
			Protocol p = (Protocol) adaptableObject;
			if (adapterType == ProtocolClass.class) return adapterType.cast(p.getProtocolClass());
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { 
				ProtocolClass.class, 
				SubWellFeature.class
		};
	}

}
