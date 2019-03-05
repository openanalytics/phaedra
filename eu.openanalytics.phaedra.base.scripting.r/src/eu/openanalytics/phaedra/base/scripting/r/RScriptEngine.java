package eu.openanalytics.phaedra.base.scripting.r;

import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.statet.rj.data.RObject;
import org.eclipse.statet.rj.servi.RServi;

import eu.openanalytics.phaedra.base.console.InteractiveConsole;
import eu.openanalytics.phaedra.base.r.rservi.RService;
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
				return (output == null) ? null : output.toString();
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
		return eval(script, null, null);
	}
	
	public void shutdown() {
		if (consoleSession != null) {
			RService.getInstance().closeSession(consoleSession);
			consoleSession = null;
		}
	}

	private Object eval(String script, Map<String, Object> objects, RServi session) throws ScriptException {
		if (!initialized) {
			try {
				consoleSession = RService.getInstance().createSession();
			} catch (CoreException e) {
				throw new ScriptException("R script engine is not yet ready");
			}
		}
		
		boolean closeSession = false;
		try {
			if (session == null) {
				session = RService.getInstance().createSession();
				closeSession = true;
			}
			//TODO include objects
			RObject retVal = session.evalData(script, new NullProgressMonitor());
			return retVal;
		} catch (CoreException e) {
			throw new ScriptException("Script error: " + e.getMessage());
		} finally {
			if (closeSession) RService.getInstance().closeSession(session);
		}
	}
}
