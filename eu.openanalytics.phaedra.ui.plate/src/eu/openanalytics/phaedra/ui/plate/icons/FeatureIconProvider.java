package eu.openanalytics.phaedra.ui.plate.icons;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.ui.icons.AbstractIconProvider;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class FeatureIconProvider extends AbstractIconProvider<Feature> {
	@Override
	public Class<Feature> getType() {
		return Feature.class;
	}

	@Override
	public ImageDescriptor getDefaultImageDescriptor() {
		return IconManager.getIconDescriptor("tag_blue.png");
	}

	@Override
	public ImageDescriptor getCreateImageDescriptor() {
		return IconManager.getIconDescriptor("tag_blue_add.png");
	}

	@Override
	public ImageDescriptor getDeleteImageDescriptor() {
		return IconManager.getIconDescriptor("tag_blue_delete.png");
	}

	@Override
	public ImageDescriptor getUpdateImageDescriptor() {
		return IconManager.getIconDescriptor("tag_blue_edit.png");
	}
}
