package eu.openanalytics.phaedra.ui.protocol.icons;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.ui.icons.AbstractIconProvider;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class ProtocolIconProvider extends AbstractIconProvider<Protocol> {
	@Override
	public Class<Protocol> getType() {
		return Protocol.class;
	}

	@Override
	public ImageDescriptor getDefaultImageDescriptor() {
		return IconManager.getIconDescriptor("struct_g.png");
	}

	@Override
	public ImageDescriptor getCreateImageDescriptor() {
		return IconManager.getIconDescriptor("struct_g_add.png");
	}

	@Override
	public ImageDescriptor getDeleteImageDescriptor() {
		return IconManager.getIconDescriptor("struct_g_delete.png");
	}

	@Override
	public ImageDescriptor getUpdateImageDescriptor() {
		return IconManager.getIconDescriptor("struct_g_edit.png");
	}

}
