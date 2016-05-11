package eu.openanalytics.phaedra.protocol.template.internal.handler;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateException;
import eu.openanalytics.phaedra.protocol.template.model.TemplateOutput;
import eu.openanalytics.phaedra.protocol.template.model.TemplateSettings;

public interface ITemplateHandler {

	public void handle(TemplateSettings settings, TemplateOutput output, IProgressMonitor monitor) throws ProtocolTemplateException;

}
