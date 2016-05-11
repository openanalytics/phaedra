package eu.openanalytics.phaedra.base.console;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import eu.openanalytics.phaedra.base.util.io.StreamUtils;

public class LogConsole extends InteractiveConsole {
	
	public final static String NAME = "Log";
	
	public LogConsole() {
		super(NAME, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "/icons/log.png"));
	}
	
	@Override
	protected String processInput(String input) throws Exception {
		if (input.equalsIgnoreCase("quote")) {
			URLConnection conn = new URL("http://iheartquotes.com/api/v1/random?format=text&max_lines=1").openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);
			InputStream in = conn.getInputStream();
			byte[] bytes = StreamUtils.readAll(in);
			String s = new String(bytes);
			s = s.substring(0, s.indexOf('\n'));
			s = s.replace("\n", "");
			s = s.replace("\r", "");
			s = s.replace("&quot;", "'");
			s = s.trim();
			return "\"" + s + "\"";
		}
		return super.processInput(input);
	}
}
