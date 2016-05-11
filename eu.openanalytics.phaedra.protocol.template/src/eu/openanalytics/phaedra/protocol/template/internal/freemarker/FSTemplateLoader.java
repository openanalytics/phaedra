package eu.openanalytics.phaedra.protocol.template.internal.freemarker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import eu.openanalytics.phaedra.protocol.template.internal.TemplateRepository;
import freemarker.cache.TemplateLoader;

public class FSTemplateLoader implements TemplateLoader {

	@Override
	public Object findTemplateSource(String name) throws IOException {
		return TemplateRepository.templateExists(name) ? name : null;
	}

	@Override
	public Reader getReader(Object source, String encoding) throws IOException {
		InputStream input = TemplateRepository.getTemplate(source.toString());
		return new InputStreamReader(input);
	}
	
	@Override
	public long getLastModified(Object source) {
		try {
			return TemplateRepository.getLastModified(source.toString());
		} catch (IOException e) {
			return -1;
		}
	}

	@Override
	public void closeTemplateSource(Object source) throws IOException {
		// Nothing to do.
	}
}
