package eu.openanalytics.phaedra.ui.protocol;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.environment.GenericEntityService;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.util.ObjectCopyFactory;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;
import eu.openanalytics.phaedra.wellimage.ImageSettingsFactory;

public class ImageSettingsService implements IUIEventListener, IModelEventListener {

	private ImageSettings currentSettings;
	private ProtocolClass currentProtocolClass;

	private static ImageSettingsService instance;

	private ImageSettingsService() {
		ProtocolUIService.getInstance().addUIEventListener(this);
		ModelEventService.getInstance().addEventListener(this);
		ImageRenderService.getInstance().addImagePropertyChangeListener(e -> notifySettingsChanged());
		// Try to obtain an initial state.
		currentProtocolClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
		generateSettings();
		// Always retrieve image settings from me (ImageSettingsService).
		ImageSettingsFactory.setRetriever(object -> getCurrentSettings(object));
	}

	public static synchronized ImageSettingsService getInstance() {
		if (instance == null) instance = new ImageSettingsService();
		return instance;
	}

	@Override
	public void handle(UIEvent event) {
		if (event.type == EventType.FeatureSelectionChanged) {
			ProtocolClass newPClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
			// Another protocol class is selected: discard current settings.
			if (currentProtocolClass == null || !currentProtocolClass.equals(newPClass)) {
				currentProtocolClass = newPClass;
				generateSettings();
				fireEvent();
			}
		}
	}

	@Override
	public void handleEvent(ModelEvent event) {
		if (event.type == ModelEventType.ObjectChanged && event.source instanceof ProtocolClass) {
			// Image settings may have changed: discard the current settings.
			ProtocolClass pClass = (ProtocolClass)event.source;
			if (pClass.equals(currentProtocolClass)) resetSettings();
		}
	}

	public ImageSettings getCurrentSettings(PlatformObject object) {
		ProtocolClass pclass = null;
		if (object != null) pclass = SelectionUtils.getAsClass(object, ProtocolClass.class);

		if (pclass == null) {
			// Could be anything... just return the active settings.
			// Note: this is used by the ImageSettingsView to just get the active settings,
			// regardless of current protocol class.
			return currentSettings;
		} else if (pclass.equals(currentProtocolClass)) {
			// This object is compatible with the current protocol class.
			return currentSettings;
		} else {
			// This object is from another protocol class.
			return pclass.getImageSettings();
		}
	}

	public void notifySettingsChanged() {
		cleanCache();
		fireEvent();
	}

	public void resetSettings() {
		generateSettings();
		notifySettingsChanged();
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private void generateSettings() {
		currentSettings = new ImageSettings();
		currentSettings.setImageChannels(new ArrayList<ImageChannel>());
		if (currentProtocolClass != null) {
			// Fix: saving channels in a new protocol class doesn't immediately show correct sequence nrs.
			List<ImageChannel> channels = currentProtocolClass.getImageSettings().getImageChannels();
			for (ImageChannel ch: channels) {
				if (ch.getSequence() == 0) GenericEntityService.getInstance().refreshEntity(ch);
			}
			ObjectCopyFactory.copySettings(currentProtocolClass.getImageSettings(), currentSettings, true);
		}
	}

	private void fireEvent() {
		UIEvent event = new UIEvent(EventType.ImageSettingsChanged);
		ProtocolUIService.getInstance().post(event);
	}

	/**
	 * Remove Images from the image cache for the current Protocol Class.
	 */
	private void cleanCache() {
		ProtocolClass pClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
		ImageRenderService.getInstance().clearCache(pClass);
	}

}
