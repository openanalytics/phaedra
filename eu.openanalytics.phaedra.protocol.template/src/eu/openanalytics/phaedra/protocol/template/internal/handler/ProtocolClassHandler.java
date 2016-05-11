package eu.openanalytics.phaedra.protocol.template.internal.handler;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.imaging.jp2k.comp.LabelComponent;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.LookupComponent;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.ManualLookupComponent;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.OverlayComponent;
import eu.openanalytics.phaedra.base.imaging.jp2k.comp.RawComponent;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Group;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateException;
import eu.openanalytics.phaedra.protocol.template.model.TemplateOutput;
import eu.openanalytics.phaedra.protocol.template.model.TemplateSettings;

/**
 * This handler takes care of the ProtocolClass fields, i.e.
 * fields that are not related to the data capture configuration.
 */
public class ProtocolClassHandler extends BaseTemplateHandler {

	@Override
	public void handle(TemplateSettings settings, TemplateOutput output, IProgressMonitor monitor) throws ProtocolTemplateException {
		monitor.beginTask("Processing: " + this.getClass().getSimpleName(), 10);
		
		String protocolName = settings.get(TemplateSettings.PROTOCOL_NAME);
		if (protocolName == null || protocolName.isEmpty()) throw new ProtocolTemplateException("Invalid template: no protocol name provided");
		
		String protocolClassName = settings.get(TemplateSettings.PROTOCOLCLASS_NAME);
		if (protocolClassName == null || protocolClassName.isEmpty()) protocolClassName = protocolName;
		
		output.protocolClass = ProtocolService.getInstance().createProtocolClass();
		output.protocolClass.setName(protocolClassName);
		
		int channelCount = settings.getGroupCount(TemplateSettings.IMAGEDATA_CHANNEL);
		for (int i=1; i<=channelCount; i++) {
			ImageChannel channel = ProtocolService.getInstance().createChannel(output.protocolClass.getImageSettings());
			channel.setImageSettings(output.protocolClass.getImageSettings());
			
			String name = settings.getGroupProperty(TemplateSettings.IMAGEDATA_CHANNEL, i, TemplateSettings.IMAGEDATA_CHANNEL_NAME);
			if (name != null) channel.setName(name);
			
			String type = settings.getGroupProperty(TemplateSettings.IMAGEDATA_CHANNEL, i, TemplateSettings.IMAGEDATA_CHANNEL_TYPE);
			if (type == null) type = "raw";
			String compressionType = "r53";
			
			int depth = 0;
			String depthString = settings.getGroupProperty(TemplateSettings.IMAGEDATA_CHANNEL, i, TemplateSettings.IMAGEDATA_CHANNEL_DEPTH);
			if (depthString != null && NumberUtils.isNumeric(depthString)) depth = Integer.parseInt(depthString);
			
			switch (type.toLowerCase()) {
			case "raw": 
				channel.setType(new RawComponent().getId());
				compressionType = "i97";
				if (depth == 0) depth = 16;
				break;
			case "overlay":
				channel.setType(new OverlayComponent().getId());
				if (depth == 0) depth = 1;
				break;
			case "label":
				channel.setType(new LabelComponent().getId());
				if (depth == 0) depth = 16;
				break;
			case "lookup":
				channel.setType(new LookupComponent().getId());
				if (depth == 0) depth = 8;
				break;
			case "manuallookup":
				channel.setType(new ManualLookupComponent().getId());
				if (depth == 0) depth = 8;
				break;
			}
			
			int[] contrast = {0, (int)Math.pow(2, depth)};
			String stringVal = settings.getGroupProperty(TemplateSettings.IMAGEDATA_CHANNEL, i, TemplateSettings.IMAGEDATA_CHANNEL_CONTRAST_MIN);
			if (stringVal != null && NumberUtils.isNumeric(stringVal))  contrast[0] = Integer.parseInt(stringVal);
			stringVal = settings.getGroupProperty(TemplateSettings.IMAGEDATA_CHANNEL, i, TemplateSettings.IMAGEDATA_CHANNEL_CONTRAST_MAX);
			if (stringVal != null && NumberUtils.isNumeric(stringVal))  contrast[1] = Integer.parseInt(stringVal);
			channel.setLevelMin(contrast[0]);
			channel.setLevelMax(contrast[1]);
			
			String colorString = settings.getGroupProperty(TemplateSettings.IMAGEDATA_CHANNEL, i, TemplateSettings.IMAGEDATA_CHANNEL_COLOR);
			RGB color = ColorUtils.parseColorString(colorString);
			if (color != null) channel.getChannelConfig().put("colorMask", ColorUtils.createRGBString(color));
			
			// This is passed on to the template Freemarker XML
			settings.setProperty(TemplateSettings.IMAGEDATA_CHANNEL + "." + i + "." + TemplateSettings.IMAGEDATA_CHANNEL_TYPE, compressionType);
			
			channel.setBitDepth(depth);
			output.protocolClass.getImageSettings().getImageChannels().add(channel);
		}
		
		int wellFeatureCount = settings.getGroupCount(TemplateSettings.WELL_FEATURE);
		for (int i=1; i<=wellFeatureCount; i++) {
			Feature feature = ProtocolService.getInstance().createFeature(output.protocolClass);
			String name = settings.getGroupProperty(TemplateSettings.WELL_FEATURE, i, TemplateSettings.WELL_FEATURE_NAME);
			feature.setName(name);
			boolean isKey = Boolean.valueOf(settings.getGroupProperty(TemplateSettings.WELL_FEATURE, i, TemplateSettings.WELL_FEATURE_KEY));
			feature.setKey(isKey);
			boolean isNumeric = Boolean.valueOf(settings.getGroupProperty(TemplateSettings.WELL_FEATURE, i, TemplateSettings.WELL_FEATURE_NUMERIC));
			feature.setNumeric(isNumeric);
			output.protocolClass.getFeatures().add(feature);
		}
		
		int subwellFeatureCount = settings.getGroupCount(TemplateSettings.SUBWELL_FEATURE);
		for (int i=1; i<=subwellFeatureCount; i++) {
			SubWellFeature feature = ProtocolService.getInstance().createSubWellFeature(output.protocolClass);
			String name = settings.getGroupProperty(TemplateSettings.SUBWELL_FEATURE, i, TemplateSettings.SUBWELL_FEATURE_NAME);
			feature.setName(name);
			boolean isKey = Boolean.valueOf(settings.getGroupProperty(TemplateSettings.SUBWELL_FEATURE, i, TemplateSettings.SUBWELL_FEATURE_KEY));
			feature.setKey(isKey);
			boolean isNumeric = Boolean.valueOf(settings.getGroupProperty(TemplateSettings.SUBWELL_FEATURE, i, TemplateSettings.SUBWELL_FEATURE_NUMERIC));
			feature.setNumeric(isNumeric);
			output.protocolClass.getSubWellFeatures().add(feature);
		}
		
		output.protocol = ProtocolService.getInstance().createProtocol(output.protocolClass);
		output.protocol.setName(protocolName);
		
		String teamCode = settings.get(TemplateSettings.PROTOCOL_TEAM);
		if (teamCode == null || teamCode.isEmpty()) {
			teamCode = SecurityService.getInstance()
					.getTeams(SecurityService.getInstance().getCurrentUserName())
					.stream().findFirst().orElse(Group.GLOBAL_TEAM);
		} else {
			teamCode = teamCode.toUpperCase();
		}
		output.protocol.setTeamCode(teamCode);
		
		monitor.done();
	}

}
