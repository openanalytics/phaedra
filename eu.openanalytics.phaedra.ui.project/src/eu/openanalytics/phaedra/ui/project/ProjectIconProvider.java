package eu.openanalytics.phaedra.ui.project;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.ui.icons.AbstractIconProvider;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.project.vo.Project;

public class ProjectIconProvider extends AbstractIconProvider<Project> {


	public ProjectIconProvider() {
	}


	@Override
	public Class<Project> getType() {
		return Project.class;
	}

	@Override
	public ImageDescriptor getDefaultImageDescriptor() {
		return IconManager.getIconDescriptor("project.png");
	}
	
	@Override
	public ImageDescriptor getCreateImageDescriptor() {
		return IconManager.getIconDescriptor("project_add.png");
	}
	
	@Override
	public ImageDescriptor getDeleteImageDescriptor() {
		return IconManager.getIconDescriptor("delete.png");
	}
	
	@Override
	public ImageDescriptor getUpdateImageDescriptor() {
		return IconManager.getIconDescriptor("pencil.png");
	}

}
