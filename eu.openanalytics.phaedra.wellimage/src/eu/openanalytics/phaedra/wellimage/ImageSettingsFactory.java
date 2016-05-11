package eu.openanalytics.phaedra.wellimage;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ImageSettingsFactory {

	private static ImageSettingsRetriever currentRetriever = object -> {
		ProtocolClass pc = ProtocolUtils.getProtocolClass(object);
		if (pc != null) return pc.getImageSettings();
		return null;
	};
	
	public static ImageSettings getSettings(PlatformObject object) {
		return currentRetriever.get(object);
	}
	
	public static void setRetriever(ImageSettingsRetriever currentRetriever) {
		ImageSettingsFactory.currentRetriever = currentRetriever;
	}
	
	public static interface ImageSettingsRetriever {
		public ImageSettings get(PlatformObject object);
	}
}
