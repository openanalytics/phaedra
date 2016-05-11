package eu.openanalytics.phaedra.protocol.template.internal.handler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateException;
import eu.openanalytics.phaedra.protocol.template.internal.freemarker.DataModelConverter;
import eu.openanalytics.phaedra.protocol.template.internal.freemarker.FreeMarkerEngine;
import eu.openanalytics.phaedra.protocol.template.model.TemplateOutput;
import eu.openanalytics.phaedra.protocol.template.model.TemplateSettings;
import freemarker.template.TemplateException;

public class DataCaptureHandler extends BaseTemplateHandler {

	@Override
	public void handle(TemplateSettings settings, TemplateOutput output, IProgressMonitor monitor) throws ProtocolTemplateException {
		String templateId = settings.get(TemplateSettings.TEMPLATE_ID);
		Object dataModel = DataModelConverter.convert(settings);
		
		try (StringWriter writer = new StringWriter()) {
			FreeMarkerEngine.processTemplate(templateId, dataModel, writer);
			output.dataCaptureConfiguration = writer.toString();
		} catch (FileNotFoundException e) {
			throw new ProtocolTemplateException(e.getMessage(), e);
		} catch (IOException e) {
			throw new ProtocolTemplateException("Failed to write data capture configuration", e);
		} catch (TemplateException e) {
			throw new ProtocolTemplateException("Template error: " + e.getMessageWithoutStackTop(), e);
		}
	}

}
