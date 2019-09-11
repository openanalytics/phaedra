package eu.openanalytics.phaedra.datacapture.hook;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import eu.openanalytics.phaedra.base.hook.BaseHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.Activator;

public class PostCaptureScriptHook extends BaseHook {

	private static final String DC_SCRIPT_MAP = "/dc/post.capture";
	private static final Pattern SCRIPT_NAME_PATTERN = Pattern.compile("(\\d+)\\..*");
	
	@Override
	public void post(IHookArguments args) {
		DataCaptureHookArguments dcArgs = (DataCaptureHookArguments) args;
		
		List<String> scriptNames = null;
		try {
			scriptNames = ScriptService.getInstance().getCatalog().getAvailableScripts(DC_SCRIPT_MAP);
		} catch (IOException e) {
			throw new RuntimeException("Failed to list post-capture scripts at " + DC_SCRIPT_MAP, e);
		}
		if (scriptNames == null || scriptNames.isEmpty()) return;
		
		EclipseLog.debug(String.format("Running %d post-capture scripts", scriptNames.size()), PostCaptureScriptHook.class);
		
		Function<String,Integer> getPriority = s -> {
			Matcher m = SCRIPT_NAME_PATTERN.matcher(s);
			return m.matches() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
		};
		
		scriptNames.sort((s1, s2) -> {
			int prio1 = getPriority.apply(s1);
			int prio2 = getPriority.apply(s2);
			if (prio1 == prio2) return s1.compareTo(s2);
			return prio2 - prio1;
		});
		
		Map<String, Object> scriptContext = new HashMap<String, Object>();
		scriptContext.put("ctx", dcArgs.context);
		scriptContext.put("reading", dcArgs.reading);
		scriptContext.put("plate", dcArgs.plate);
		
		long start = System.currentTimeMillis();
		for (String scriptName: scriptNames) {
			try {
				ScriptService.getInstance().getCatalog().run(scriptName, scriptContext);
			} catch (ScriptException e) {
				EclipseLog.error("Post-capture script failed: " + scriptName, e, Activator.PLUGIN_ID);
			}
		}
		long duration = System.currentTimeMillis() - start;
		EclipseLog.debug(String.format("%d post-capture scripts executed in %d ms", scriptNames.size(), duration), PostCaptureScriptHook.class);
	}

}
