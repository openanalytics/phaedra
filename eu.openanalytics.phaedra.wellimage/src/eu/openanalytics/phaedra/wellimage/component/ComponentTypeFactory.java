package eu.openanalytics.phaedra.wellimage.component;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.imaging.jp2k.comp.IComponentType;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.LabelComponent;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.LookupComponent;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.ManualLookupComponent;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.OverlayComponent;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.RawComponent;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;

public class ComponentTypeFactory {

	private Map<Long, IComponentType> componentCache;
	private Map<Integer, IComponentType> componentTypes;
	
	private static ComponentTypeFactory instance;
	
	private ComponentTypeFactory() {
		// Hidden constructor.
		componentCache = new HashMap<>();
		componentTypes = new HashMap<>();
		loadKnownTypes();
	}
	
	public static ComponentTypeFactory getInstance() {
		if (instance == null) instance = new ComponentTypeFactory();
		return instance;
	}
	
	public IComponentType getComponent(ImageChannel channel) {
		long channelId = channel.getId();
		if (channelId != 0 && componentCache.containsKey(channelId)) {
			return componentCache.get(channelId);
		}
		IComponentType component = loadComponent(channel);
		componentCache.put(channelId, component);
		return component;
	}
	
	public IComponentType[] getKnownTypes() {
		return componentTypes.values().toArray(new IComponentType[componentTypes.size()]);
	}
	
	public void purgeCache(ImageChannel channel) {
		componentCache.remove(channel.getId());
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private IComponentType loadComponent(ImageChannel channel) {
		IComponentType type = componentTypes.get(channel.getType());
		try {
			// Create a new instance, because the IComponentType itself
			// may be a stateful object and cannot be shared between ImageChannels.
			
			Class<? extends IComponentType> componentClass = type.getClass();
			IComponentType component = componentClass.newInstance();
			component.loadConfig(channel.getChannelConfig());
			return component;
		} catch (Exception e) {
			throw new RuntimeException("Failed to load image component of type " + channel.getType());
		}
	}
	
	private void loadKnownTypes() {
		addKnownType(RawComponent.class);
		addKnownType(OverlayComponent.class);
		addKnownType(ManualLookupComponent.class);
		addKnownType(LabelComponent.class);
		addKnownType(LookupComponent.class);
	}
	
	private void addKnownType(Class<? extends IComponentType> typeClass) {
		try {
			IComponentType type = typeClass.newInstance();
			componentTypes.put(type.getId(), type);
		} catch (Exception e) {
			// Ignore invalid types.
		}
	}
}
