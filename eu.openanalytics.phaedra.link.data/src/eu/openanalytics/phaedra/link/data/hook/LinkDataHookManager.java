package eu.openanalytics.phaedra.link.data.hook;

import java.util.List;

import eu.openanalytics.phaedra.base.hook.HookService;
import eu.openanalytics.phaedra.base.hook.IBatchedHookArguments;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.data.Activator;
import eu.openanalytics.phaedra.link.data.DataLinkTask;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkException;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class LinkDataHookManager {

	public final static String HOOK_POINT_ID = Activator.PLUGIN_ID + ".dataHookPoint";

	public static void startLinkBatch(DataLinkTask task, List<PlateReading> readings) {
		IBatchedHookArguments args = new LinkDataBatchedHookArguments(task, readings);
		HookService.getInstance().startBatch(HOOK_POINT_ID, args);
	}
	
	public static void preLink(DataLinkTask task, PlateReading reading) throws PlateLinkException {
		Plate plate = task.createNewPlates ? null : task.mappedReadings.get(reading);
		IHookArguments args = new LinkDataHookArguments(task, reading, plate);
		try {
			HookService.getInstance().runPre(HOOK_POINT_ID, args);
		} catch (PreHookException e) {
			throw new PlateLinkException(e.getMessage(), e.getCause());
		}
	}
	
	public static void postLink(DataLinkTask task, PlateReading reading, Plate plate) {
		IHookArguments args = new LinkDataHookArguments(task, reading, plate);
		HookService.getInstance().runPost(HOOK_POINT_ID, args);
	}
	
	public static void endLinkBatch(boolean successful) {
		HookService.getInstance().endBatch(HOOK_POINT_ID, successful);
	}
}
