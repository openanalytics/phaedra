package eu.openanalytics.phaedra.link.platedef.template;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

public interface ITemplateManager {

	/**
	 * Load the template with the given unique identifier.
	 * 
	 * @param id The template's unique identifier.
	 * @return The template, or null if no matching template was found.
	 * @throws IOException If the template cannot be loaded.
	 */
	public PlateTemplate getTemplate(String id) throws IOException;

	/**
	 * Get a list of template ids for the given protocol class.
	 * 
	 * @param protocolClassId The id of the protocol class to get templates for.
	 * @return A list of matching template ids.
	 * @throws IOException If the templates cannot be accessed.
	 */
	public List<String> getTemplateIds(long protocolClassId) throws IOException;
	
	/**
	 * Get a list of all templates for the given protocol class.
	 * 
	 * @param protocolClassId The id of the protocol class to get templates for.
	 * @return A list of matching templates.
	 * @throws IOException If the templates cannot be loaded.
	 */
	public List<PlateTemplate> getTemplates(long protocolClassId) throws IOException;

	/**
	 * Check if a template already exists for the given identifier.
	 * 
	 * @param id The id to check for.
	 * @return True if a template already exists for the given identifier.
	 */
	public boolean exists(String id);

	/**
	 * Check if the current user has permission to delete the given template.
	 * 
	 * @param template The template to check.
	 * @return True if the current user can delete the template.
	 */
	public boolean canDelete(PlateTemplate template);

	/**
	 * Delete the given template.
	 * 
	 * @param template The template to delete.
	 * @throws IOException If the template cannot be deleted.
	 */
	public void delete(PlateTemplate template) throws IOException;

	/**
	 * Save the given template, which can be either a new template or a modified existing template.
	 * 
	 * @param template The template to save.
	 * @param monitor An optional monitor to track progress.
	 * @param isNew True if the template is a new template.
	 * @throws IOException If the template cannot be saved.
	 */
	public void save(PlateTemplate template, IProgressMonitor monitor, boolean isNew) throws IOException;
}
