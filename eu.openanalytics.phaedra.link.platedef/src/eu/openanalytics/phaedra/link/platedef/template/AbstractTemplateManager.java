package eu.openanalytics.phaedra.link.platedef.template;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.security.SecurityService;

public abstract class AbstractTemplateManager implements ITemplateManager {

	@Override
	public boolean canDelete(PlateTemplate template) {
		String creator = template.getCreator();
		if (creator == null) return false;
		String currentUser = SecurityService.getInstance().getCurrentUserName();
		return (creator.equalsIgnoreCase(currentUser));
	}

	@Override
	public void delete(PlateTemplate template) throws IOException {
		if (canDelete(template)) {
			doDelete(template);
		}
	}

	@Override
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
		boolean exists = exists(id);
		if (isNew && exists) {
			throw new IllegalArgumentException("Template '" + id + "' already exists.");
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
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TemplateParser.write(template, out);
		
		if (exists) doUpdate(template, out.toString());
		else doCreate(template, out.toString());
		
		if (monitor != null) monitor.done();
	}

	protected abstract void doDelete(PlateTemplate template) throws IOException;
	protected abstract void doCreate(PlateTemplate template, String xmlData) throws IOException;
	protected abstract void doUpdate(PlateTemplate template, String xmlData) throws IOException;

}
