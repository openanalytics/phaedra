package eu.openanalytics.phaedra.protocol.template.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateException;

public class TemplateSettings extends Properties {

	private static final long serialVersionUID = -8839622299009452043L;
	
	public final static String PROTOCOLCLASS_NAME = "protocolclass.name";
	public final static String PROTOCOL_NAME = "protocol.name";
	public final static String PROTOCOL_TEAM = "protocol.team";
	
	public final static String TEMPLATE_ID = "template";
	
	public final static String MONTAGE_LAYOUT = "montage.layout";
	
	public final static String PLATE_FILEPATTERN = "plate.filepattern";
	public final static String PLATE_FOLDERPATTERN = "plate.folderpattern";
	
	public final static String WELLDATA_PATH = "welldata.path";
	public final static String WELLDATA_PATTERN = "welldata.filepattern";
	
	public final static String SUBWELLDATA_PATH = "subwelldata.path";
	public final static String SUBWELLDATA_PATTERN = "subwelldata.filepattern";
	
	public final static String IMAGEDATA_PATH = "imagedata.path";
	public final static String IMAGEDATA_CHANNEL = "imagedata.channel";
	public final static String IMAGEDATA_CHANNEL_NAME = "name";
	public final static String IMAGEDATA_CHANNEL_TYPE = "type";
	public final static String IMAGEDATA_CHANNEL_DEPTH = "depth";
	public final static String IMAGEDATA_CHANNEL_PATTERN = "filepattern";

	public final static String WELL_FEATURE = "wellfeature";
	public final static String WELL_FEATURE_NAME = "name";
	public final static String WELL_FEATURE_KEY = "key";
	public final static String WELL_FEATURE_NUMERIC = "numeric";
	
	public final static String SUBWELL_FEATURE = "subwellfeature";
	public final static String SUBWELL_FEATURE_NAME = "name";
	public final static String SUBWELL_FEATURE_KEY = "key";
	public final static String SUBWELL_FEATURE_NUMERIC = "numeric";
	
	// Raw channels only
	public final static String IMAGEDATA_CHANNEL_CONTRAST_MIN = "contrast.min";
	public final static String IMAGEDATA_CHANNEL_CONTRAST_MAX = "contrast.max";
	
	// Raw and Overlay channels only
	public final static String IMAGEDATA_CHANNEL_COLOR = "color";
	
	public TemplateSettings(InputStream input) throws ProtocolTemplateException {
		try {
			// Properties will remove single backslashes as they are line separators.
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			StreamUtils.copy(input, out);
			String contents = new String(out.toByteArray());
			contents = contents.replace("\\", "\\\\");
			
			load(new ByteArrayInputStream(contents.getBytes()));
		} catch (IOException e) {
			throw new ProtocolTemplateException("Failed to load template: invalid format", e);
		}
	}
	
	public List<String> getAllProperties() {
		return keySet().stream()
			.map(key -> (String)key)
			.collect(Collectors.toList());
	}
	
	public String get(String name) {
		return getProperty(name);
	}
	
	public int getGroupCount(String groupName) {
		long channelCount = keySet().stream()
			.map(key -> (String)key)
			.filter(key -> key.startsWith(groupName))
			.map(key -> key.substring(groupName.length() + 1))
			.map(key -> key.substring(0, key.indexOf('.')))
			.mapToInt(key -> Integer.parseInt(key))
			.distinct()
			.count();
		return (int)channelCount;
	}
	
	public List<String> getGroupProperties(String groupName, int groupNr) {
		return keySet().stream()
			.map(key -> (String)key)
			.filter(key -> key.startsWith(groupName))
			.map(key -> key.substring(groupName.length() + 1))
			.collect(Collectors.toList());
	}
	
	public String getGroupProperty(String groupName, int groupNr, String name) {
		return get(groupName + "." + groupNr + "." + name);
	}
}
