package eu.openanalytics.phaedra.base.scripting.api;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.URLStreamHandlerService;

import eu.openanalytics.phaedra.base.scripting.Activator;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

public class ScriptCatalog {
	
	private final static String FS_SUBPATH = "/script.catalog";
	
	private String fsPath;
	
	public ScriptCatalog(String fsPath) {
		this.fsPath = fsPath;
		
		// Register a custom protocol handler for urls which redirect to the ScriptCatalog.
		ScriptCatalogURLHandler urlHandler = new ScriptCatalogURLHandler();
		BundleContext ctx = Activator.getDefault().getBundle().getBundleContext();
		ctx.registerService(URLStreamHandlerService.class, urlHandler, urlHandler.getProperties());
	}
	
	public String getScriptBody(String scriptName) throws IOException {
		File scriptFile = getScriptFile(scriptName);
		if (scriptFile == null) return null;
		return new String(StreamUtils.readAll(scriptFile.getAbsolutePath()));
	}
	
	public Object run(String name, Object[] args) throws ScriptException {
		return run(name, args, false);
	}
	
	public Object run(String name, Object[] args, boolean async) throws ScriptException {
		Map<String, Object> argMap = new HashMap<>();
		argMap.put("args", args);
		return run(name, argMap, async);
	}
	
	public Object run(String name, Map<String,Object> args) throws ScriptException {
		return run(name, args, false);
	}
	
	public Object run(String name, final Map<String,Object> args, boolean async) throws ScriptException {
		File scriptFile = getScriptFile(name);
		if (scriptFile == null) throw new ScriptException("Script not found: " + name);
		
		try {
			final String scriptBody = new String(StreamUtils.readAll(scriptFile.getAbsolutePath()));
			final String engineId = ScriptService.getInstance().getEngineIdForFile(scriptFile.getName());
			
			if (async) {
				Job scriptJob = new Job("Running script '" + name + "'") {
					protected IStatus run(IProgressMonitor monitor) {
						try {
							ScriptService.getInstance().executeScript(scriptBody, args, engineId);
						} catch (Throwable t) {
							return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Script error: " + t.getMessage(), t);
						}
						return Status.OK_STATUS;
					};
				};
				scriptJob.setUser(false);
				scriptJob.schedule();
				return null;
			} else {
				return ScriptService.getInstance().executeScript(scriptBody, args, engineId);
			}
		} catch (IOException e) {
			throw new ScriptException("Cannot load script " + name + ": " + e.getMessage());
		}
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private File getScriptFile(String name) {
		File file = new File(fsPath + FS_SUBPATH + "/" + name);
		if (file.exists()) return file;
		// The name does not contain an extension, try appending one of the supported extensions.
		String[] supportedTypes = ScriptService.getInstance().getSupportedFileTypes();
		for (String type: supportedTypes) {
			file = new File(fsPath + FS_SUBPATH + "/" + name + "." + type);
			if (file.exists()) return file;
		}
		return null;
	}
}
