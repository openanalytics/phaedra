package eu.openanalytics.phaedra.ui.protocol.template.simple;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateService;

public class SimpleProtocolAnalyzer {

	public static final String SCRIPT_ANALYZE_WELLDATA = "protocol/simple.protocol.welldata.analyzer.js";
	public static final String SCRIPT_ANALYZE_SUBWELLDATA = "protocol/simple.protocol.subwelldata.analyzer.js";
	public static final String SCRIPT_ANALYZE_IMAGEDATA = "protocol/simple.protocol.imagedata.analyzer.js";
	public static final String SCRIPT_CREATE_TEMPLATE = "protocol/simple.protocol.template.creator.js";
	
	public static boolean analyzeFile(SimpleProtocolWizardState state, String file, String scriptName, IWizardContainer container) {
		try {
			container.run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						monitor.beginTask("Analyzing file " + FileUtils.getName(file), IProgressMonitor.UNKNOWN);
						Map<String,Object> params = new HashMap<>();
						params.put("monitor", monitor);
						params.put("parameters", state.parameters);
						ScriptService.getInstance().getCatalog().run(scriptName, params);
						monitor.done();
					} catch (ScriptException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
			return true;
		} catch (Exception e) {
			String errMessage = (e instanceof InvocationTargetException && e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
			MessageDialog.openError(container.getShell(), "Failed to analyze file", errMessage);
			return false;
		}
	}
	
	public static Protocol createProtocol(SimpleProtocolWizardState state) {
		try {
			StringBuilder template = new StringBuilder();
			Map<String,Object> params = new HashMap<>();
			params.put("template", template);
			params.put("parameters", state.parameters);
			ScriptService.getInstance().getCatalog().run(SCRIPT_CREATE_TEMPLATE, params);
			return ProtocolTemplateService.getInstance().executeTemplate(template.toString(), new NullProgressMonitor());
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					"Failed to create protocol",
					"An error occurred while creating the protocol:\n" + e.getMessage());
			return null;
		}
	}
}
