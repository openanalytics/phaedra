package eu.openanalytics.phaedra.link.platedef;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.link.platedef.hook.LinkPlateDefHookManager;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkException;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;
import eu.openanalytics.phaedra.link.platedef.source.AbstractDefinitionSource;
import eu.openanalytics.phaedra.link.platedef.source.IPlateDefinitionSource;
import eu.openanalytics.phaedra.link.platedef.source.SourceRegistry;
import eu.openanalytics.phaedra.link.platedef.template.DBTemplateManager;
import eu.openanalytics.phaedra.link.platedef.template.FSTemplateManager;
import eu.openanalytics.phaedra.link.platedef.template.ITemplateManager;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

/**
* <p>
* This service interacts with various plate definition systems, and
* enables the linking of plates with plate definitions from those systems.
* </p><p>
* At least one plate definition system is always available: the plate template
* system. For more information, see {@link #getTemplateManager()}
* </p>
*/
public class PlateDefinitionService {

	private static PlateDefinitionService instance = new PlateDefinitionService();
	
	private SourceRegistry sourceRegistry;
	private ITemplateManager templateManager;
	
	private PlateDefinitionService() {
		// Hidden constructor
		sourceRegistry = new SourceRegistry();
		
		String templateStorage = Screening.getEnvironment().getConfig().getValue("plate.template.storage");
		if ("db".equalsIgnoreCase(templateStorage)) {
			templateManager = new DBTemplateManager();
		} else {
			templateManager = new FSTemplateManager();
		}
	}
	
	public static PlateDefinitionService getInstance() {
		return instance;
	}
	
	/*
	 * **********
	 * Public API
	 * **********
	 */
	
	public String[] getSourceIds() {
		return sourceRegistry.getIds();
	}
	
	public IPlateDefinitionSource getSource(String id) {
		return sourceRegistry.getSource(id);
	}
	
	public synchronized String linkSource(IPlateDefinitionSource source, PlateLinkSettings settings) throws PlateLinkException {
		LinkPlateDefHookManager.preLink(source.getId(), settings);
		String retVal = source.link(settings);
		if (AbstractDefinitionSource.STATUS_OK.equalsIgnoreCase(retVal)) {
			// Invoke the post link operations (calculation etc) only if the link was successful.
			LinkPlateDefHookManager.postLink(source.getId(), settings);
		}
		return retVal;
	}
	
	public synchronized String[] linkSource(IPlateDefinitionSource source, PlateLinkSettings[] batchSettings, IProgressMonitor monitor) {
		if (monitor == null) monitor = new NullProgressMonitor();
		
		String[] retVals = new String[batchSettings.length];
		LinkPlateDefHookManager.startLinkBatch();
		for (int i=0; i<batchSettings.length; i++) {
			PlateLinkSettings settings = batchSettings[i];
			monitor.subTask("Linking plate " + settings.getPlate().toString());
			try {
				retVals[i] = linkSource(source, settings);
			} catch (PlateLinkException e) {
				retVals[i] = AbstractDefinitionSource.STATUS_ERROR + ": " + e.getMessage();
			}
			if (i%2 == 0) monitor.worked(1);
		}
		monitor.subTask("Recalculating plates");
		LinkPlateDefHookManager.endLinkBatch();
		return retVals;
	}
	
	public ITemplateManager getTemplateManager() {
		return templateManager;
	}
	
	public boolean checkLinkPermission(Plate plate) {
		if (SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, plate)) {
			return true;
		} else {
			String message = "No permission to modify plate.";
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Cannot Link", "Cannot link " + plate + ":\n" + message);
			return false;
		}
		// Note: the validation status is not checked here, but via a pre-link hook.
	}
}
