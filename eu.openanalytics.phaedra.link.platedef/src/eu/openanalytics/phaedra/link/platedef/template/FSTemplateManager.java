package eu.openanalytics.phaedra.link.platedef.template;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.link.platedef.Activator;

public class FSTemplateManager extends AbstractTemplateManager {

	public static final String TEMPLATE_REPO_PATH = "/plate.templates";
	
	private static final PlateTemplate UNLOADED_TEMPLATE = new PlateTemplate();
	
	private Map<String, PlateTemplate> templateCache = new ConcurrentHashMap<String, PlateTemplate>();

	public FSTemplateManager() {
		loadTemplateIds();
	}

	@Override
	public PlateTemplate getTemplate(String id) throws IOException {
		PlateTemplate template = templateCache.get(id);
		if (template == UNLOADED_TEMPLATE) {
			template = loadTemplate(id);
			templateCache.put(id, template);
		}
		return template;
	}

	@Override
	public List<PlateTemplate> getTemplates(long protocolClassId) throws IOException {
		loadTemplates();
		return templateCache.values().stream()
				.filter(t -> t.getProtocolClassId() == protocolClassId)
				.sorted(PlateTemplateSorter.ById)
				.collect(Collectors.toList());
	}

	@Override
	public boolean exists(String id) {
		String path = TEMPLATE_REPO_PATH + "/" + id + ".xml";
		try {
			return Screening.getEnvironment().getFileServer().exists(path) && !Screening.getEnvironment().getFileServer().isDirectory(path);
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	protected void doDelete(PlateTemplate template) throws IOException {
		String path = TEMPLATE_REPO_PATH + "/" + template.getId() + ".xml";
		Screening.getEnvironment().getFileServer().delete(path);
		templateCache.remove(template.getId());		
	}
	
	@Override
	protected void doCreate(PlateTemplate template, String xmlData) throws IOException {
		String path = TEMPLATE_REPO_PATH + "/" + template.getId() + ".xml";
		Screening.getEnvironment().getFileServer().putContents(path, xmlData.getBytes());
		// Update or add the template in the cache.
		templateCache.put(template.getId(), template);
	}
	
	@Override
	protected void doUpdate(PlateTemplate template, String xmlData) throws IOException {
		// Simply overwrite the existing file
		doCreate(template, xmlData);
	}

	/*
	 * Non-public
	 * **********
	 */

	private void loadTemplateIds() {
		String path = TEMPLATE_REPO_PATH;
		try {
			if (!Screening.getEnvironment().getFileServer().isDirectory(path)) return;
			Screening.getEnvironment().getFileServer().dir(path)
				.stream()
				.filter(item -> item.endsWith(".xml"))
				.forEach(item -> templateCache.put(item.substring(0,item.lastIndexOf('.')), UNLOADED_TEMPLATE));
		} catch (IOException e) {
			// Template dir unavailable.
			EclipseLog.error("Error loading plate templates", e, Activator.getDefault());
		}
	}

	private void loadTemplates() {
		templateCache.keySet().parallelStream().forEach(id -> {
			try {
				if (templateCache.get(id) == UNLOADED_TEMPLATE) templateCache.put(id, loadTemplate(id));
			} catch (IOException e) {
				// Skip invalid templates.
				EclipseLog.warn("Ignored invalid template: " + id, Activator.getDefault());
				templateCache.remove(id);
			}
		});
	}
	
	private PlateTemplate loadTemplate(String id) throws IOException {
		String path = TEMPLATE_REPO_PATH + "/" + id + ".xml";
		if (exists(id)) {
			InputStream in = Screening.getEnvironment().getFileServer().getContents(path);
			PlateTemplate template = TemplateParser.parse(in);
			return template;
		}
		return null;
	}

	private enum PlateTemplateSorter implements Comparator<PlateTemplate> {
		ById {
			@Override
			public int compare(PlateTemplate o1, PlateTemplate o2) {
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;

				return o1.getId().compareTo(o2.getId());
			}
		};
	}
}
