package eu.openanalytics.phaedra.link.platedef.template;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.link.platedef.Activator;


public class TemplateManager {

	public final static String TEMPLATE_REPO_PATH = "/plate.templates";

	private Map<String, PlateTemplate> templateCache;

	public TemplateManager() {
		templateCache = new ConcurrentHashMap<String, PlateTemplate>();
		loadTemplates();
	}

	public String[] getIDs() {
		List<String> ids = new ArrayList<String>(templateCache.keySet());
		Collections.sort(ids);
		return ids.toArray(new String[ids.size()]);
	}

	public PlateTemplate getTemplate(String id) throws IOException {
		PlateTemplate template = templateCache.get(id);
		if (template == null) {
			template = loadTemplate(id);
			templateCache.put(id, template);
		}
		return template;
	}

	public List<PlateTemplate> getTemplates(long protocolClassId) throws IOException {
		List<PlateTemplate> filteredTemplates = new ArrayList<PlateTemplate>();
		for (PlateTemplate t: templateCache.values()) {
			if (t.getProtocolClassId() == protocolClassId) filteredTemplates.add(t);
		}
		Collections.sort(filteredTemplates, PlateTemplateSorter.ById);
		return filteredTemplates;
	}

	public boolean exists(String id) {
		String path = TEMPLATE_REPO_PATH + "/" + id + ".xml";
		try {
			return Screening.getEnvironment().getFileServer().exists(path) && !Screening.getEnvironment().getFileServer().isDirectory(path);
		} catch (IOException e) {
			return false;
		}
	}

	public boolean canDelete(PlateTemplate template) {
		String creator = template.getCreator();
		if (creator == null) return false;
		String currentUser = SecurityService.getInstance().getCurrentUserName();
		return (creator.equalsIgnoreCase(currentUser));
	}

	public void delete(PlateTemplate template) throws IOException {
		if (canDelete(template)) {
			String path = TEMPLATE_REPO_PATH + "/" + template.getId() + ".xml";
			Screening.getEnvironment().getFileServer().delete(path);
			templateCache.remove(template.getId());
		}
	}

	public void save(PlateTemplate template, IProgressMonitor monitor, boolean isNew) throws IOException {
		if (monitor != null) monitor.beginTask("Saving template", 10);
		String id = template.getId();

		// First, check whether the id is valid.
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("No template id set");

		if (isNew) {
			String creator = SecurityService.getInstance().getCurrentUserName();
			template.setCreator(creator);
		}

		// If we're not allowed to overwrite (in the case of new templates),
		// check the availability of the id, and throw an error if it's taken.
		if (isNew && exists(id)) {
			throw new IllegalArgumentException("Template id '" + id + "' already exists.");
		}

		// If the size decreased, throw away unused wells.
		int size = template.getRows() * template.getColumns();
		int wellCount = template.getWells().size();
		if (size < wellCount) {
			List<Integer> nrsToDiscard = new ArrayList<Integer>();
			for (Integer nr: template.getWells().keySet()) {
				if (nr > size) nrsToDiscard.add(nr);
			}
			for (Integer nr: nrsToDiscard) {
				template.getWells().remove(nr);
			}
		}

		// Make sure compound types are set to default when omitted.
		String defaultType = "OC";
		for (WellTemplate well: template.getWells().values()) {
			String compType = well.getCompoundType();
			String compNr = well.getCompoundNumber();
			if ((compType == null || compType.isEmpty()) && (compNr != null && !compNr.isEmpty())) well.setCompoundType(defaultType);
		}

		// Then, write the template to XML.
		if (monitor != null) monitor.subTask("Writing template");
		String path = TEMPLATE_REPO_PATH + "/" + id + ".xml";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TemplateParser.write(template, out);
		Screening.getEnvironment().getFileServer().putContents(path, out.toByteArray());

		// Update or add the template in the cache.
		templateCache.put(id, template);

		if (monitor != null) monitor.done();
	}

	/*
	 * Non-public
	 * **********
	 */

	private void loadTemplates() {
		templateCache.clear();
		String path = TEMPLATE_REPO_PATH;
		try {
			List<String> items = Screening.getEnvironment().getFileServer().dir(path);
			items.parallelStream().forEach(item -> {
				try {
					String id = item.substring(0,item.lastIndexOf('.'));
					templateCache.put(id, loadTemplate(id));
				} catch (Exception e) {
					// Skip invalid templates.
					EclipseLog.warn("Ignored invalid template: " + item, Activator.getDefault());
				}
			});
		} catch (IOException e) {
			// Template dir unavailable.
			EclipseLog.error("Error loading plate templates", e, Activator.getDefault());
		}
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
