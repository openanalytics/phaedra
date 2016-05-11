package eu.openanalytics.phaedra.model.plate.hook;

import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.hook.HookService;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.model.plate.Activator;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class WellDataActionHookManager {
	
	public final static String HOOK_POINT_ID = Activator.PLUGIN_ID + ".wellDataActionHookPoint";

	public static void startBatch() {
		HookService.getInstance().startBatch(HOOK_POINT_ID, null);
	}
	
	public static void preAction(Plate plate, Feature feature, Object values, boolean normalized, ModelEventType action) throws RuntimeException {
		IHookArguments args = new WellDataActionHookArguments(plate, feature, values, normalized, action);
		try {
			HookService.getInstance().runPre(HOOK_POINT_ID, args);
		} catch (PreHookException e) {
			throw new RuntimeException(e.getMessage(), e.getCause());
		}
	}
	
	public static void postAction(Plate plate, Feature feature, Object values, boolean normalized, ModelEventType action) {
		IHookArguments args = new WellDataActionHookArguments(plate, feature, values, normalized, action);
		HookService.getInstance().runPost(HOOK_POINT_ID, args);
	}
	
	public static void endBatch(boolean successful) {
		HookService.getInstance().endBatch(HOOK_POINT_ID, successful);
	}
}
