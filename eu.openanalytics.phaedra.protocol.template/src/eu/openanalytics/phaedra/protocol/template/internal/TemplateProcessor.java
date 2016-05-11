package eu.openanalytics.phaedra.protocol.template.internal;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateException;
import eu.openanalytics.phaedra.protocol.template.internal.handler.DataCaptureHandler;
import eu.openanalytics.phaedra.protocol.template.internal.handler.ProtocolClassHandler;
import eu.openanalytics.phaedra.protocol.template.model.TemplateOutput;
import eu.openanalytics.phaedra.protocol.template.model.TemplateSettings;

public class TemplateProcessor {

	public Protocol process(InputStream input, IProgressMonitor monitor) throws ProtocolTemplateException {
		monitor.beginTask("Processing protocol template", 100);
		
		if (monitor.isCanceled()) return null;
		monitor.subTask("Parsing template");
		TemplateSettings settings = new TemplateSettings(input);
		monitor.worked(10);

		if (monitor.isCanceled()) return null;
		TemplateOutput output = new TemplateOutput();
		
		monitor.subTask("Creating protocol class");
		if (monitor.isCanceled()) return null;
		ProtocolClassHandler pClassHandler = new ProtocolClassHandler();
		pClassHandler.handle(settings, output, new SubProgressMonitor(monitor, 30));
		
		if (monitor.isCanceled()) return null;
		DataCaptureHandler dcHandler = new DataCaptureHandler();
		dcHandler.handle(settings, output, new SubProgressMonitor(monitor, 30));
		
		if (monitor.isCanceled()) return null;
		monitor.subTask("Saving protocol class");
		ProtocolService.getInstance().updateProtocolClass(output.protocolClass);
		ProtocolService.getInstance().updateProtocol(output.protocol);
		monitor.worked(15);
		
		if (monitor.isCanceled()) return null;
		monitor.subTask("Saving capture configuration");
		try {
			String captureConfig = "protocolclass-" + output.protocolClass.getId() + ".capture";
			//TODO Use ModuleFactory.saveCaptureConfig() instead
			String savePath = "/data.capture.configurations/" + captureConfig + ".xml";
			Screening.getEnvironment().getFileServer().putContents(savePath, output.dataCaptureConfiguration.getBytes());
			
			output.protocolClass.setDefaultCaptureConfig(captureConfig);
			ProtocolService.getInstance().updateProtocolClass(output.protocolClass);
		} catch (IOException e) {
			throw new ProtocolTemplateException("Failed to save capture configuration", e);
		}
		monitor.worked(15);
		
		monitor.done();
		return output.protocol;
	}
}
