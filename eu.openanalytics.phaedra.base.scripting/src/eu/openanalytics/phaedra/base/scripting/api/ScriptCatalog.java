package eu.openanalytics.phaedra.base.scripting.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.URLStreamHandlerService;

import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.scripting.Activator;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

public class ScriptCatalog {
	
	private final static String FS_SUBPATH = "/script.catalog";
	
	private SecureFileServer fs;
	
	public ScriptCatalog(SecureFileServer fs) {
		this.fs = fs;
		
		// Register a custom protocol handler for urls which redirect to the ScriptCatalog.
		ScriptCatalogURLHandler urlHandler = new ScriptCatalogURLHandler();
		BundleContext ctx = Activator.getDefault().getBundle().getBundleContext();
		ctx.registerService(URLStreamHandlerService.class, urlHandler, urlHandler.getProperties());
	}
	
	public List<String> getAvailableScripts(String map) throws IOException {
		return listScripts(FS_SUBPATH + "/" + map);
	}
	
	public String getScriptBody(String scriptName) throws IOException {
		try (InputStream script = getScript(scriptName)) {
			if (script == null) return null;
			return new String(StreamUtils.readAll(script));
		}
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
		try (InputStream script = getScript(name)) {
			if (script == null) throw new ScriptException("Script not found: " + name);

			final String scriptBody = new String(StreamUtils.readAll(script));
			final String engineId = ScriptService.getInstance().getEngineIdForFile(name);
			
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
	
	private List<String> listScripts(String map) throws IOException {
		List<String> scriptFiles = new ArrayList<>();
		for (String item: fs.dir(map)) {
			if (fs.isDirectory(item)) scriptFiles.addAll(listScripts(""));
			else scriptFiles.add((map + "/" + item).substring(FS_SUBPATH.length() + 1));
		}
		return scriptFiles;
	}
	
	private InputStream getScript(String name) throws IOException {
		String path = FS_SUBPATH + "/" + name;
		if (fs.exists(path)) return fs.getContents(path);
		
		// The name does not contain an extension, try appending one of the supported extensions.
		String[] supportedTypes = ScriptService.getInstance().getSupportedFileTypes();
		for (String type: supportedTypes) {
			path = FS_SUBPATH + "/" + name + "." + type;
			if (fs.exists(path)) return fs.getContents(path);
		}
		return null;
	}
}
