package eu.openanalytics.phaedra.ui.plate.icons;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.ui.icons.AbstractIconProvider;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;

public class FeatureValueIconProvider extends AbstractIconProvider<FeatureValue> {
	@Override
	public Class<FeatureValue> getType() {
		return FeatureValue.class;
	}

	@Override
	public ImageDescriptor getDefaultImageDescriptor() {
		return IconManager.getIconDescriptor("explorer/view_contents.png");
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
