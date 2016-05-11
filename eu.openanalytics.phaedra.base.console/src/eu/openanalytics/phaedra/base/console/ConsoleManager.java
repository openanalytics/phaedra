package eu.openanalytics.phaedra.base.console;

import java.util.ArrayList;
import java.util.List;


public class ConsoleManager {
	
	private static ConsoleManager instance;
	
	private List<InteractiveConsole> consoles;
	private InteractiveConsole logConsole;
	
	private ConsoleManager() {
		consoles = new ArrayList<>();
		logConsole = new LogConsole();
		consoles.add(logConsole);
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
	
	public void print(String message) {
		print(LogConsole.NAME, message);
	}
	
	public void printErr(String message) {
		printErr(LogConsole.NAME, message);
	}
	
	public void printErr(String message, Throwable cause) {
		printErr(LogConsole.NAME, message, cause);
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
