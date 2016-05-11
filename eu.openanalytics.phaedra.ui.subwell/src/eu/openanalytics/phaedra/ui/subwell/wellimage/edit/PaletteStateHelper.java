package eu.openanalytics.phaedra.ui.subwell.wellimage.edit;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.user.UserService;
import eu.openanalytics.phaedra.ui.subwell.Activator;

public class PaletteStateHelper {

	private final static String PREF_TYPE = "Memento";
	private final static String ROOT_TAG = "palette-state";
	
	public static void restoreState(IPaletteTool palette) {
		try {
			String mementoString = UserService.getInstance().getPreferenceValue(PREF_TYPE, getPalettePrefKey(palette));
			if (mementoString != null) {
				XMLMemento memento = XMLMemento.createReadRoot(new StringReader(mementoString));
				palette.restoreState(memento);
			}
		} catch (WorkbenchException e) {
			EclipseLog.warn("Failed to restore state for palette " + palette.getLabel(), e, Activator.getDefault());
		}
	}
	
	public static void saveState(IPaletteTool palette) {
		try {
			XMLMemento memento = XMLMemento.createWriteRoot(ROOT_TAG);
			palette.saveState(memento);
			StringWriter writer = new StringWriter();
			memento.save(writer);
			String value = writer.getBuffer().toString();
			UserService.getInstance().setPreferenceValue(PREF_TYPE, getPalettePrefKey(palette), value);
		} catch (IOException e) {
			EclipseLog.warn("Failed to save state for palette " + palette.getLabel(), e, Activator.getDefault());
		}
	}
	
	public static SubWellFeature getSubWellFeature(IMemento memento, String key) {
		String featureIdString = memento.getString(key);
		if (featureIdString == null) return null;
		try {
			long featureId = Long.parseLong(featureIdString);
			return ProtocolService.getInstance().getSubWellFeature(featureId);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static void saveSubWellFeature(SubWellFeature f, IMemento memento, String key) {
		if (f != null) memento.putString(key, f.getId() + "");
	}
	
	private static String getPalettePrefKey(IPaletteTool palette) {
		return "palette" + palette.getClass().getSimpleName();
	}
}
