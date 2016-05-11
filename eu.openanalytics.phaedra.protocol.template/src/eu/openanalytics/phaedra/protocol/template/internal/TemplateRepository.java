package eu.openanalytics.phaedra.protocol.template.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.environment.Screening;

public class TemplateRepository {

	private final static String TEMPLATE_DIR = "/data.capture.templates";
	private final static String TEMPLATE_NAME_SUFFIX = ".template.xml";
	private final static String TEMPLATE_KEYS_SUFFIX = ".template.keys.txt";
	private final static String TEMPLATE_EXAMPLE_SUFFIX = ".template.example.txt";
	
	public static String[] getAvailableTemplateIds() throws IOException {
		List<String> validIds = Screening.getEnvironment().getFileServer().dir(TEMPLATE_DIR).stream()
				.filter(t -> t.endsWith(TEMPLATE_NAME_SUFFIX))
				.map(t -> t.substring(0, t.indexOf(TEMPLATE_NAME_SUFFIX)))
				.collect(Collectors.toList());
		return validIds.toArray(new String[validIds.size()]);
	}
	
	public static boolean templateExists(String templateId) throws IOException {
		String path = TEMPLATE_DIR + "/" + templateId + TEMPLATE_NAME_SUFFIX;
		return Screening.getEnvironment().getFileServer().exists(path);
	}
	
	public static long getLastModified(String templateId) throws IOException {
		String path = TEMPLATE_DIR + "/" + templateId + TEMPLATE_NAME_SUFFIX;
		return Screening.getEnvironment().getFileServer().getLastModified(path);
	}
	
	public static InputStream getTemplate(String templateId) throws IOException {
		String path = TEMPLATE_DIR + "/" + templateId + TEMPLATE_NAME_SUFFIX;
		return Screening.getEnvironment().getFileServer().getContents(path);
	}
	
	public static InputStream getSettingKeys(String templateId) throws IOException {
		String path = TEMPLATE_DIR + "/" + templateId + TEMPLATE_KEYS_SUFFIX;
		return Screening.getEnvironment().getFileServer().getContents(path);
	}
	
	public static String getExampleSettings(String templateId) throws IOException {
		String path = TEMPLATE_DIR + "/" + templateId + TEMPLATE_EXAMPLE_SUFFIX;
		if (Screening.getEnvironment().getFileServer().exists(path)) {
			return Screening.getEnvironment().getFileServer().getContentsAsString(path);
		}
		return null;
	}
}
