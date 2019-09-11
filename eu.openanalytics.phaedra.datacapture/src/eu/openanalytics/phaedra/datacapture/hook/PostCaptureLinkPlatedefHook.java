package eu.openanalytics.phaedra.datacapture.hook;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hook.BaseHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.util.VariableResolver;
import eu.openanalytics.phaedra.link.platedef.PlateDefinitionService;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkException;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;
import eu.openanalytics.phaedra.link.platedef.source.IPlateDefinitionSource;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;

public class PostCaptureLinkPlatedefHook extends BaseHook {

	private static final String SETTING_AUTO_LINK = "datacapture.auto.link";
	private static final String SETTING_SOURCE_ID = "datacapture.auto.link.source";
	
	@Override
	public void post(IHookArguments args) {
		DataCaptureHookArguments dcArgs = (DataCaptureHookArguments) args;
		if (shouldLinkPlatedef(dcArgs)) {
			try {
				linkPlatedef(dcArgs);
			} catch (PlateLinkException e) {
				throw new RuntimeException("Failed to auto-link plate: " + dcArgs.plate, e);
			}
		}
	}
	
	private void linkPlatedef(DataCaptureHookArguments dcArgs) throws PlateLinkException {
		EclipseLog.debug(String.format("Auto-linking plate %s", dcArgs.plate.getBarcode()), PostCaptureLinkPlatedefHook.class);
		
		PlateLinkSettings settings = new PlateLinkSettings();
		settings.setPlate(dcArgs.plate);
		settings.setBarcode(dcArgs.plate.getBarcode());
		
		String sourceId = getSourceId(dcArgs);
		if (sourceId == null) throw new PlateLinkException("Cannot auto-link: no source ID provided");
		IPlateDefinitionSource source = PlateDefinitionService.getInstance().getSource(sourceId);
		if (source == null) throw new PlateLinkException("Cannot auto-link: source with ID '" + sourceId + "' not found");
		
		PlateDefinitionService.getInstance().linkSource(source, settings);
	}
	
	private String getSourceId(DataCaptureHookArguments args) {
		String sourceId = getSetting(SETTING_SOURCE_ID, args);
		if (sourceId == null) sourceId = ProtocolUtils.getProtocolClass(args.plate).getDefaultLinkSource();
		return sourceId;
	}
	
	private boolean shouldLinkPlatedef(DataCaptureHookArguments args) {
		return Boolean.valueOf(getSetting(SETTING_AUTO_LINK, args));
	}
	
	private String getSetting(String name, DataCaptureHookArguments args) {
		// Prio 1: system property
		String settingValue = System.getProperty(name);
		if (settingValue != null) return settingValue;
		
		// Prio 2: config.xml setting
		settingValue = Screening.getEnvironment().getConfig().getValue(name);
		if (settingValue != null && !settingValue.isEmpty()) return settingValue;
		
		// Prio 3: DC parameter
		Object var = VariableResolver.get("${" + name + "}", args.context);
		if (var != null) return String.valueOf(var);
		
		return null;
	}
}
