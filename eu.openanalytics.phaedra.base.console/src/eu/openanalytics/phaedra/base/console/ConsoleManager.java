package eu.openanalytics.phaedra.base.console;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;


public class ConsoleManager {
	
	private static ConsoleManager instance;
	
	private List<InteractiveConsole> consoles;
	
	private ConsoleManager() {
		consoles = new ArrayList<>();
	}
	
	public static ConsoleManager getInstance() {
		if (instance == null) {
			instance = new ConsoleManager();
		}
		return instance;
	}

	public void registerConsole(InteractiveConsole console) {
		consoles.add(console);
	}

	public InteractiveConsole[] getConsoles() {
		return consoles.toArray(new InteractiveConsole[consoles.size()]);
	}
	
	@Deprecated
	public void print(String message) {
		EclipseLog.info(message, Activator.PLUGIN_ID);
	}
	
	@Deprecated
	public void printErr(String message) {
		EclipseLog.error(message, null, Activator.PLUGIN_ID);
	}
	
	public void print(String console, String message) {
		InteractiveConsole c = getConsole(console);
		if (c != null) c.print(message);
	}
	
	public void printErr(String console, String message) {
		InteractiveConsole c = getConsole(console);
		if (c != null) c.printErr(message);
	}
	
	public void printErr(String console, String message, Throwable cause) {
		InteractiveConsole c = getConsole(console);
		if (c != null) c.printErr(message, cause);
	}
	
	private InteractiveConsole getConsole(String name) {
		for (InteractiveConsole c: consoles) {
			if (c.getName().equals(name)) return c;
		}
		return null;
	}
}
