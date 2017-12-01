package eu.openanalytics.phaedra.base.scripting.engine;

import java.io.IOException;
import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.console.InteractiveConsole;
import eu.openanalytics.phaedra.base.scripting.Activator;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

public abstract class BaseScriptEngine implements IScriptEngine, IExecutableExtension {

	private String id;
	private String label;
	private boolean isDefault;
	private String fileExtension;
	private InteractiveConsole console;
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
	
	@Override
	public boolean isDefault() {
		return isDefault;
	}
	
	@Override
	public String getFileExtension() {
		return fileExtension;
	}
	
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		this.id = config.getAttribute(IScriptEngine.ATTR_ID);
		this.label = config.getAttribute(IScriptEngine.ATTR_LABEL);
		this.fileExtension = config.getAttribute(IScriptEngine.ATTR_FILE_EXT);
		String isDefaultString = config.getAttribute(IScriptEngine.ATTR_IS_DEFAULT);
		if ("true".equals(isDefaultString)) isDefault = true;
	}
	
	@Override
	public InteractiveConsole getConsole() {
		return console;
	}
	
	@Override
	public void registerAPI(String name, Object value, String help) {
		throw new UnsupportedOperationException(getId() + " does not support registration of API objects");
	}
	
	@Override
	public Object eval(String script) throws ScriptException {
		return eval(script, null);
	}
	
	@Override
	public Object evalFile(String filePath) throws ScriptException {
		return evalFile(filePath, null);
	}
	
	@Override
	public Object evalFile(String filePath, Map<String, Object> objects) throws ScriptException {
		try {
			byte[] contents = StreamUtils.readAll(filePath);
			String body = new String(contents);
			return eval(body, objects);
		} catch (IOException e) {
			throw new ScriptException("Failed to load script at " + filePath + ": " + e.getMessage());
		}
	}
	
	@Override
	public Dialog createScriptEditor(Shell parentShell, StringBuilder script) {
		// Default: no editor
		return null;
	}
	
	protected void setConsole(InteractiveConsole console) {
		this.console = console;
	}
	
	protected ImageDescriptor getImageDescriptor(String pluginId, String imagePath) {
		ImageDescriptor[] desc = new ImageDescriptor[1];
		Display.getDefault().syncExec(() -> {
			desc[0] = Activator.imageDescriptorFromPlugin(pluginId, imagePath);
		});
		return desc[0];
	}
}
