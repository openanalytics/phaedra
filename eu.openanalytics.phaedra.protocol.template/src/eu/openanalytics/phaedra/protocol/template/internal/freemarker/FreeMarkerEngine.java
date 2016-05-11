package eu.openanalytics.phaedra.protocol.template.internal.freemarker;

import java.io.IOException;
import java.io.Writer;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreeMarkerEngine {

	@SuppressWarnings("deprecation")
	private static Configuration config = new Configuration();
	
	static {
		config.setTemplateLoader(new FSTemplateLoader());
	}
	
	public static void processTemplate(String id, Object dataModel, Writer writer) throws IOException, TemplateException {
		Template template = config.getTemplate(id);
		template.process(dataModel, writer);
		writer.flush();
	}
}
