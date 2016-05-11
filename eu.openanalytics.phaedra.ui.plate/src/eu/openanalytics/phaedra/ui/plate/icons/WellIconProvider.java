package eu.openanalytics.phaedra.ui.plate.icons;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.ui.icons.AbstractIconProvider;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellIconProvider extends AbstractIconProvider<Well> {
	@Override
	public Class<Well> getType() {
		return Well.class;
	}

	@Override
	public ImageDescriptor getDefaultImageDescriptor() {
		return IconManager.getIconDescriptor("image.png");
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
