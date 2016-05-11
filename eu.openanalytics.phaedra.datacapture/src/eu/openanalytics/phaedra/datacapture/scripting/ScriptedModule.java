package eu.openanalytics.phaedra.datacapture.scripting;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.environment.IEnvironment;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.module.AbstractModule;

public class ScriptedModule extends AbstractModule {

	public final static String TYPE = "ScriptedModule";
	
	private final static String MODULE_REPO_PATH = "/data.capture.modules";
	
	private String script;
	private String scriptType;

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void configure(ModuleConfig cfg) throws DataCaptureException {
		super.configure(cfg);
		
		// Attempt to load the script from the module repo.
		String scriptId = (String)cfg.getParameters().getParameter("script.id");
		try {
			String[] script = loadScript(scriptId);
			if (script == null) throw new DataCaptureException("Scripted module " + scriptId + " not found");
			this.scriptType = script[0];
			this.script = script[1];
		} catch (IOException e) {
			throw new DataCaptureException("Failed to load module script " + scriptId, e);
		}
	}

	@Override
	public void execute(DataCaptureContext context, IProgressMonitor monitor) throws DataCaptureException {
		
		if (script == null || script.isEmpty()) {
			throw new DataCaptureException("Scripted module error: no script set");
		}
		
		DataCaptureTask task = context.getTask();
		Map<String, Object> scriptContext = new HashMap<String, Object>();
		scriptContext.put("ctx", context);
		scriptContext.put("config", getConfig());
		scriptContext.put("task", task);
		scriptContext.put("monitor", monitor);
		
		try {
			ScriptService.getInstance().executeScript(script, scriptContext, scriptType);
		} catch (ScriptException e) {
			Throwable cause = e.getCause();
			if (cause == null) cause = e;
			throw new DataCaptureException("Script error: " + cause.getMessage(), e);
		}
	}

	private String[] loadScript(String id) throws IOException {
		IEnvironment env = Screening.getEnvironment();
		if (env == null) return null;
		
		List<String> items = env.getFileServer().dir(MODULE_REPO_PATH);
		for (String item: items) {
			String path = MODULE_REPO_PATH + "/" + item;
			if (env.getFileServer().isDirectory(path)) continue;

			String engineId = ScriptService.getInstance().getEngineIdForFile(item);
			if (engineId == null) continue;
			
			String itemName = item.substring(0, item.lastIndexOf('.'));
			if (itemName.equalsIgnoreCase(id)) {
				String scriptBody = env.getFileServer().getContentsAsString(path);
				return new String[] {engineId, scriptBody};
			}
		}
		
		return null;
	}
}
