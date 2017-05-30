package eu.openanalytics.phaedra.base.environment.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;

public class Config {

	private Document doc;
	private PasswordStore pwdStore;
	private String[] environments;
	
	private static final String SETTING_PWD_PATH = "pwd.path";
	
	public Config(InputStream input) throws IOException {
		doc = XmlUtils.parse(input);
		
		String pwdPath = XmlUtils.findString("/config/settings/setting[@name=\"" + SETTING_PWD_PATH + "\"]", doc);
		if (pwdPath != null && !pwdPath.isEmpty()) pwdStore = new PasswordStore(pwdPath);
		
		NodeList tags = XmlUtils.findTags("/config/environments/environment", doc);
		environments = new String[tags.getLength()];
		for (int i = 0; i < tags.getLength(); i++) {
			environments[i] = ((Element) tags.item(i)).getAttribute("name");
		}
	}
	
	public String[] getEnvironments() {
		return environments;
	}
	
	public boolean hasCategory(String env, String category) {
		return XmlUtils.findTags(createCategoryXPath(env, category), doc).getLength() > 0;
	}
	
	public String getValue(String setting) {
		return resolveVars(XmlUtils.findString("/config/settings/setting[@name=\"" + setting + "\"]", doc));
	}
	
	public String getValue(String env, String category, String property) {
		return resolveVars(XmlUtils.findString(createCategoryXPath(env, category) + "/" + property, doc));
	}
	
	public String resolvePassword(String env, String category) throws IOException {
		Element pwTag = XmlUtils.getFirstElement(createCategoryXPath(env, category) + "/password", doc);
		if (pwTag == null) return null;
		String password = XmlUtils.getNodeValue(pwTag);
		if (password == null || password.isEmpty()) {
			String source = pwTag.getAttribute("source");
			if ("pwd".equalsIgnoreCase(source)) password = resolvePassword(pwTag.getAttribute("id"));
		}
		return password;
	}
	
	public String resolvePassword(String id) throws IOException {
		if (pwdStore == null) throw new IOException("Cannot retrieve password: setting " + SETTING_PWD_PATH + " is undefined");
		return pwdStore.getPassword(id);
	}
	
	private String createCategoryXPath(String env, String category) {
		return "/config/environments/environment[@name=\""+env+"\"]/" + category;
	}
	
	private String resolveVars(String value) {
		return StringUtils.resolveVariables(value, varName -> {
			if (varName.equalsIgnoreCase("workspace")) {
				try {
					String workspace = new URL(System.getProperty("osgi.instance.area")).getFile();
					if (workspace.endsWith("/")) workspace = workspace.substring(0, workspace.length() - 1);
					if (ProcessUtils.isWindows() && workspace.startsWith("/")) workspace = workspace.substring(1);
					return workspace;
				} catch (MalformedURLException e) {}
			}
			return System.getProperty(varName);
		});
	}
}
