package eu.openanalytics.phaedra.ui.plate.icons;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.ui.icons.AbstractIconProvider;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public class ExperimentIconProvider extends AbstractIconProvider<Experiment> {
	@Override
	public Class<Experiment> getType() {
		return Experiment.class;
	}

	@Override
	public ImageDescriptor getDefaultImageDescriptor() {
		return IconManager.getIconDescriptor("map.png");
	}

	@Override
	public ImageDescriptor getCreateImageDescriptor() {
		return IconManager.getIconDescriptor("map_add.png");
	}

	@Override
	public ImageDescriptor getDeleteImageDescriptor() {
		return IconManager.getIconDescriptor("map_delete.png");
	}

	@Override
	public ImageDescriptor getUpdateImageDescriptor() {
		return IconManager.getIconDescriptor("map_edit.png");
	}

}
