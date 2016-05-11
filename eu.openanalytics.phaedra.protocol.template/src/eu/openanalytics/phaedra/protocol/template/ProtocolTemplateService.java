package eu.openanalytics.phaedra.protocol.template;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.protocol.template.internal.TemplateProcessor;
import eu.openanalytics.phaedra.protocol.template.internal.TemplateRepository;
import eu.openanalytics.phaedra.protocol.template.validation.TemplateSettingsValidator;
import eu.openanalytics.phaedra.protocol.template.validation.ValidationOutcome;

public class ProtocolTemplateService {

	private static ProtocolTemplateService instance;
	
	private ProtocolTemplateService() {
		// Hidden constructor
	}
	
	public static synchronized ProtocolTemplateService getInstance() {
		if (instance == null) instance = new ProtocolTemplateService();
		return instance;
	}

	/**
	 * Get the IDs of all available protocol templates.
	 */
	public String[] getAvailableTemplateIds() {
		try {
			return TemplateRepository.getAvailableTemplateIds();
		} catch (IOException e) {
			EclipseLog.error("Failed to list protocol templates", e, Activator.getDefault());
			return new String[0];
		}
	}

	/**
	 * Get a set of example settings for the given template ID.
	 * 
	 * @param templateId The template ID to get an example for.
	 * @return Example settings, or null if no example is available.
	 */
	public String getExampleSettings(String templateId) {
		try {
			return TemplateRepository.getExampleSettings(templateId);
		} catch (IOException e) {
			EclipseLog.error("Failed to retrieve example settings for " + templateId, e, Activator.getDefault());
			return null;
		}
	}
	
	public ValidationOutcome validateSettings(String settings) {
		TemplateSettingsValidator validator = new TemplateSettingsValidator();
		return validator.validate(settings);
	}
	
	public Protocol executeTemplate(String settings, IProgressMonitor monitor) throws ProtocolTemplateException {
		if (settings == null) throw new ProtocolTemplateException("No template settings provided");
		return executeTemplate(new ByteArrayInputStream(settings.getBytes()), monitor);
	}
	
	/**
	 * Run the template specified by the provided settings.
	 * 
	 * @param settings The template settings.
	 * @param monitor A progress monitor, may be null.
	 * @return The protocol created by the template.
	 * @throws ProtocolTemplateException If the template fails to run for any reason.
	 */
	public Protocol executeTemplate(InputStream settings, IProgressMonitor monitor) throws ProtocolTemplateException {
		SecurityService.getInstance().checkWithException(Permissions.PROTOCOLCLASS_CREATE, null);
		if (monitor == null) monitor = new NullProgressMonitor();
		
		TemplateProcessor processor = new TemplateProcessor();
		return processor.process(settings, monitor);
	}
}
