package eu.openanalytics.phaedra.ui.protocol.icons;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.ui.icons.AbstractIconProvider;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ProtocolClassIconProvider extends AbstractIconProvider<ProtocolClass> {
	@Override
	public Class<ProtocolClass> getType() {
		return ProtocolClass.class;
	}

	@Override
	public ImageDescriptor getDefaultImageDescriptor() {
		return IconManager.getIconDescriptor("struct.png");
	}

	@Override
	public ImageDescriptor getCreateImageDescriptor() {
		return IconManager.getIconDescriptor("struct_add.png");
	}

	@Override
	public ImageDescriptor getDeleteImageDescriptor() {
		return IconManager.getIconDescriptor("struct_delete.png");
	}

	@Override
	public ImageDescriptor getUpdateImageDescriptor() {
		return IconManager.getIconDescriptor("struct_edit.png");
	}

}
