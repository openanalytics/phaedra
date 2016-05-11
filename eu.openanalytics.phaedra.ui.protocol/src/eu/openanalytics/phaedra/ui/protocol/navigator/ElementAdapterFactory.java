package eu.openanalytics.phaedra.ui.protocol.navigator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;

import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;

public class ElementAdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof IElement) {
			IElement el = (IElement)adaptableObject;
			Object data = el.getData();
			if (data != null) {
				if (adapterType.isAssignableFrom(data.getClass())) {
					return adapterType.cast(data);
				} else if (data instanceof IAdaptable) {
					return ((IAdaptable)data).getAdapter(adapterType);
				}
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { 
				ProtocolClass.class, 
				Protocol.class,
				Feature.class,
				FeatureClass.class,
				ImageChannel.class,
				ImageSettings.class,
				SubWellFeature.class,
				WellType.class
		};
	}

}
