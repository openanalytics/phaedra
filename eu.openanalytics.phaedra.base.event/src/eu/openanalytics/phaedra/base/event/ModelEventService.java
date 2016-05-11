package eu.openanalytics.phaedra.base.event;

import java.util.List;

import org.eclipse.core.commands.common.EventManager;

public class ModelEventService extends EventManager {

	private static ModelEventService instance;
	
	private ModelEventService() {
		// Hidden constructor
	}
	
	public static ModelEventService getInstance() {
		if (instance == null) instance = new ModelEventService();
		return instance;
	}
	
	public void addEventListener(IModelEventListener listener) {
		addListenerObject(listener);
	}

	public void removeEventListener(IModelEventListener listener) {
		removeListenerObject(listener);
	}
	
	public void fireEvent(ModelEvent event) {
		final Object[] listeners = getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			IModelEventListener listener = (IModelEventListener)listeners[i];
			listener.handleEvent(event);
		}
	}
	
	public static Object[] getEventItems(ModelEvent event) {
		Object[] items;
		Object src = event.source;
		if (src instanceof List) {
			List<?> list = (List<?>)src;
			items = list.toArray();
		} else if (src instanceof Object[]){
			items = (Object[]) src;
		} else {
			items = new Object[]{src};
		}
		return items;
	}
}
