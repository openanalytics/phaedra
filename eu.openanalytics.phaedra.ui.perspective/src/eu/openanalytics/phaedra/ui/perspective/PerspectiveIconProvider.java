package eu.openanalytics.phaedra.ui.perspective;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.ui.icons.AbstractIconProvider;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.ui.perspective.vo.SavedPerspective;

public class PerspectiveIconProvider extends AbstractIconProvider<SavedPerspective> {
	
	@Override
	public Class<SavedPerspective> getType() {
		return SavedPerspective.class;
	}

	@Override
	public ImageDescriptor getDefaultImageDescriptor() {
		return IconManager.getIconDescriptor("application_view_gallery.png");
	}

	@Override
	public ImageDescriptor getCreateImageDescriptor() {
		return null;
	}

	@Override
	public ImageDescriptor getDeleteImageDescriptor() {
		return null;
	}

	@Override
	public ImageDescriptor getUpdateImageDescriptor() {
		return null;
	}
}
