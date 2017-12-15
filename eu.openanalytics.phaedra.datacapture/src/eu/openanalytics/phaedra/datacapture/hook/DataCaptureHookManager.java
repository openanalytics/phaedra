package eu.openanalytics.phaedra.datacapture.hook;

import eu.openanalytics.phaedra.base.hook.HookService;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.datacapture.Activator;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class DataCaptureHookManager {

	public final static String HOOK_POINT_ID = Activator.PLUGIN_ID + ".dcHookPoint";
	
	public static void preCapture(DataCaptureContext context) throws DataCaptureException {
		DataCaptureHookArguments args = new DataCaptureHookArguments();
		args.context = context;
		try {
			HookService.getInstance().runPre(HOOK_POINT_ID, args);
		} catch (PreHookException e) {
			throw new DataCaptureException(e.getMessage(), e.getCause());
		}
	}
	
	public static void postCapture(DataCaptureContext context, PlateReading reading, Plate plate) {
		DataCaptureHookArguments args = new DataCaptureHookArguments();
		args.context = context;
		args.reading = reading;
		args.plate = plate;
		HookService.getInstance().runPost(HOOK_POINT_ID, args);
	}
}
