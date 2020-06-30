package eu.openanalytics.phaedra.base.scripting.api;

import java.io.IOException;
import java.io.InputStream;

import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

public class FeatureTemplateCatalog {
	
private final static String FS_SUBPATH = "/calc.feature.templates";
	
	private SecureFileServer fs;

	public FeatureTemplateCatalog(SecureFileServer fs) {
		this.fs = fs;
	}
	
	public String getFeatureTemplate(String name) throws IOException {
		try (InputStream template = getTemplate(name)) {
			if (template == null) return null;
			return new String(StreamUtils.readAll(template));
		}
		
	}
	
	private InputStream getTemplate(String name) throws IOException {
		String path = FS_SUBPATH + "/" + name;
		if (fs.exists(path)) return fs.getContents(path);
		
		path = FS_SUBPATH + "/" + name + ".json";
		if (fs.exists(path)) 
			return fs.getContents(path);
		
		return null;
	}

}
