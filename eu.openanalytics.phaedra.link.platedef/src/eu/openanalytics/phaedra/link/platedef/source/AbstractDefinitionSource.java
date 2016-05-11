package eu.openanalytics.phaedra.link.platedef.source;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import eu.openanalytics.phaedra.link.platedef.PlateDefinitionService;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkException;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;

public abstract class AbstractDefinitionSource implements IPlateDefinitionSource {

	public final static String STATUS_ERROR = "ERROR";
	public final static String STATUS_OK = "LINKED";
	public final static String STATUS_BARCODE_NOT_FOUND = "BARCODE NOT FOUND";
	public final static String STATUS_NO_BARCODE_ENTERED = "NO BARCODE ENTERED";
	
	private String id;
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		id = config.getAttribute("id");
	}

	@Override
	public final String link(PlateLinkSettings settings) throws PlateLinkException {
		if (PlateDefinitionService.getInstance().checkLinkPermission(settings.getPlate())) {
			return doLink(settings);
		}
		else {
			throw new PlateLinkException("Failed to link: no permission to modify plate.");
		}
	}
	
	protected abstract String doLink(PlateLinkSettings settings) throws PlateLinkException;
}
