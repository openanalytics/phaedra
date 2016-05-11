package eu.openanalytics.phaedra.link.platedef.hook;

import eu.openanalytics.phaedra.base.hook.HookService;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.link.platedef.Activator;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkException;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;

public class LinkPlateDefHookManager {

	public final static String HOOK_POINT_ID = Activator.PLUGIN_ID + ".platedefHookPoint";

	public static void startLinkBatch() {
		HookService.getInstance().startBatch(HOOK_POINT_ID, null);
	}
	
	public static void preLink(String source, PlateLinkSettings settings) throws PlateLinkException {
		IHookArguments args = new LinkPlateDefHookArguments(source, settings);
		try {
			HookService.getInstance().runPre(HOOK_POINT_ID, args);
		} catch (PreHookException e) {
			throw new PlateLinkException(e.getMessage(), e.getCause());
		}
	}
	
	public static void postLink(String source, PlateLinkSettings settings) {
		IHookArguments args = new LinkPlateDefHookArguments(source, settings);
		HookService.getInstance().runPost(HOOK_POINT_ID, args);
	}
	
	public static void endLinkBatch() {
		HookService.getInstance().endBatch(HOOK_POINT_ID);
	}
}
