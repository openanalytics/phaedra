package eu.openanalytics.phaedra.ui.protocol.util;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ProtocolLabelProvider extends LabelProvider {

	private static final Image IMG_PROTOCOL_CLASS = IconManager.getIconImage("struct.png");
	private static final Image IMG_PROTOCOL = IconManager.getIconImage("struct_g.png");

	@Override
	public String getText(Object element) {
		if (element instanceof ProtocolClass) {
			ProtocolClass pc = (ProtocolClass)element;
			return pc.getName();
		} else if (element instanceof Protocol) {
			Protocol p = (Protocol)element;
			return p.getName();
		}
		return element.toString();
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof ProtocolClass) {
			return IMG_PROTOCOL_CLASS;
		} else if (element instanceof Protocol) {
			return IMG_PROTOCOL;
		}
		return null;
	}
}
