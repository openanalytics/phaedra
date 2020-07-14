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

/**
 * <p>
 * The ScriptCatalog represents a catalog of scripts which
 * are hosted on the file server and can be executed via
 * this class.
 * </p>
 * This class does not allow modification or removal of scripts.
 * Administrator privileges are required to do so.
 */
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
	
	/**
	 * Get a list of names of the available scripts in the catalog,
	 * under the specified map. If an empty String is provided, the whole
	 * catalog is returned.
	 */
	public List<String> getAvailableScripts(String map) throws IOException {
		return listScripts(FS_SUBPATH + "/" + map);
	}
	
	/**
	 * Load the body of the named script. If the script doesn't exist,
	 * null is returned instead.
	 */
	public String getScriptBody(String scriptName) throws IOException {
		try (InputStream script = getScript(scriptName)) {
			if (script == null) return null;
			return new String(StreamUtils.readAll(script));
		}
	}
	
	/**
	 * Execute the script with the specified name.
	 * The argument array will be passed into the script as a top-level object named 'args'.
	 * 
	 * See also {@link ScriptCatalog#run(String, Map, boolean)}
	 */
	public Object run(String name, Object[] args) throws ScriptException {
		return run(name, args, false);
	}
	
	/**
	 * Execute the script with the specified name.
	 * The argument array will be passed into the script as a top-level object named 'args'.
	 * 
	 * See also {@link ScriptCatalog#run(String, Map, boolean)}
	 */
	public Object run(String name, Object[] args, boolean async) throws ScriptException {
		Map<String, Object> argMap = new HashMap<>();
		argMap.put("args", args);
		return run(name, argMap, async);
	}
	
	/**
	 * See {@link ScriptCatalog#run(String, Map, boolean)}
	 */
	public Object run(String name, Map<String,Object> args) throws ScriptException {
		return run(name, args, false);
	}
	
	/**
	 * Execute the script with the specified name.
	 * 
	 * @param name The name of the script (including any folder names)
	 * @param args The arguments to pass into the script as top-level objects
	 * @param async True to run the script in a separate thread and not wait for its outcome.
	 * @return The return value of the script, or null if async is true.
	 * @throws ScriptException If the script execution fails for any reason.
	 */
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
			String path = map + "/" + item;
			if (fs.isDirectory(path)) scriptFiles.addAll(listScripts(path));
			else scriptFiles.add(path.substring(FS_SUBPATH.length() + 1));
		}
		return scriptFiles;
	}
	
	private InputStream getScript(String name) throws IOException {
		if (name.startsWith("/")) name = name.substring(1);
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
