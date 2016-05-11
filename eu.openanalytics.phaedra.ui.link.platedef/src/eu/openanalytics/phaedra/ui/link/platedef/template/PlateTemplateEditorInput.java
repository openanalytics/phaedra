package eu.openanalytics.phaedra.ui.link.platedef.template;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;


public class PlateTemplateEditorInput implements IEditorInput {

	private PlateTemplate plateTemplate;
	private boolean isNewTemplate;
	
	public PlateTemplate getPlateTemplate() {
		return plateTemplate;
	}

	public void setPlateTemplate(PlateTemplate plateTemplate) {
		this.plateTemplate = plateTemplate;
	}

	public boolean isNewTemplate() {
		return isNewTemplate;
	}

	public void setNewTemplate(boolean isNewTemplate) {
		this.isNewTemplate = isNewTemplate;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return IconManager.getIconDescriptor("grid.png");
	}

	@Override
	public String getName() {
		return plateTemplate.getId();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getName();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((plateTemplate == null) ? 0 : plateTemplate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PlateTemplateEditorInput other = (PlateTemplateEditorInput) obj;
		if (plateTemplate == null) {
			if (other.plateTemplate != null)
				return false;
		} else if (!plateTemplate.equals(other.plateTemplate))
			return false;
		return true;
	}
}
