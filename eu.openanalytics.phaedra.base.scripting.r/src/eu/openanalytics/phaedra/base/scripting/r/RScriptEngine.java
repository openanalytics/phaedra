package eu.openanalytics.phaedra.base.scripting.r;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.statet.rj.data.RList;
import org.eclipse.statet.rj.data.RObject;
import org.eclipse.statet.rj.servi.RServi;

import eu.openanalytics.phaedra.base.console.InteractiveConsole;
import eu.openanalytics.phaedra.base.r.rservi.RService;
import eu.openanalytics.phaedra.base.r.rservi.RUtils;
import eu.openanalytics.phaedra.base.scripting.engine.BaseScriptEngine;

public class RScriptEngine extends BaseScriptEngine {

	private boolean initialized;
	private RServi consoleSession;
	
	@Override
	public void initialize() throws ScriptException {
		Activator.getDefault().setRScriptEngine(this);
		
		String label = getLabel();
		if (label == null) label = getId();
		
		InteractiveConsole console = new InteractiveConsole(label, getImageDescriptor(Activator.PLUGIN_ID, "/icons/r.png")) {
			@Override
			protected String processInput(String input) throws Exception {
				Object output = eval(input, null, consoleSession);
				if (output == null) return null;
				if (output instanceof String[]) return Arrays.toString((String[]) output);
				return output.toString();
			}
		};
		setConsole(console);
	}

	@Override
	public void registerAPI(String name, Object value, String help) {
		// Ignore, R does not support API additions.
	}
	
	@Override
	public Object eval(String script, Map<String, Object> objects) throws ScriptException {
		return eval(script, objects, null);
	}
	
	public void shutdown() {
		if (consoleSession != null) {
			RService.getInstance().closeSession(consoleSession);
			consoleSession = null;
		}
	}

	private Object eval(String script, Map<String, Object> objects, RServi session) throws ScriptException {
		if (!initialized) doInitLazy();
		
		boolean closeSession = false;
		try {
			if (session == null) {
				session = RService.getInstance().createSession();
				closeSession = true;
			}
			if (objects != null) {
				for (Entry<String, Object> entry: objects.entrySet()) {
					String name = entry.getKey();
					RObject value = RUtils.makeRObject(entry.getValue(), true);
					session.assignData(name, value, new NullProgressMonitor());
				}
			}
			RObject retVal = null;
			if (script.contains("\n")) {
				byte[] scriptContents = script.getBytes();
				session.uploadFile(new ByteArrayInputStream(scriptContents), scriptContents.length, "scriptFile", 0, null);
				retVal = session.evalData("source(\"scriptFile\")", null);
				if (retVal instanceof RList) retVal = ((RList) retVal).get("value");
			} else {
				retVal = session.evalData(script, new NullProgressMonitor());
			}
			return RUtils.getAsJavaObject(retVal);
		} catch (CoreException e) {
			return Double.NaN;
//			throw new ScriptException(parseRErrorMessage(e.getMessage()));
		} finally {
			if (closeSession) RService.getInstance().closeSession(session);
		}
	}
	
	private synchronized void doInitLazy() throws ScriptException {
		if (initialized) return;
		try {
			consoleSession = RService.getInstance().createSession();
			initialized = true;
		} catch (CoreException e) {
			throw new ScriptException("R script engine is not yet ready");
		}
	}
	
	private String parseRErrorMessage(String msg) {
		String rServiMsg = "<simpleError in ";
		if (msg != null && msg.contains(rServiMsg)) {
			msg = msg.substring(msg.indexOf(rServiMsg) + rServiMsg.length());
			if (msg.length() > 2) msg = msg.substring(0, msg.length() - 2);
		}
		return msg;
	}
}
