package eu.openanalytics.phaedra.ui.plate.util;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ExperimentLabelProvider extends LabelProvider {

	private static final Image IMG_PROTOCOL_CLASS = IconManager.getIconImage("struct.png");
	private static final Image IMG_PROTOCOL = IconManager.getIconImage("struct_g.png");
	private static final Image IMG_EXPERIMENT = IconManager.getIconImage("map.png");

	@Override
	public String getText(Object element) {
		if (element instanceof ProtocolClass) {
			ProtocolClass pc = (ProtocolClass)element;
			return pc.getName();
		} else if (element instanceof Protocol) {
			Protocol p = (Protocol)element;
			return p.getName();
		} else if (element instanceof Experiment) {
			Experiment exp = (Experiment)element;
			return exp.getName();
		}
		return element.toString();
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof ProtocolClass) {
			return IMG_PROTOCOL_CLASS;
		} else if (element instanceof Protocol) {
			return IMG_PROTOCOL;
		} else if (element instanceof Experiment) {
			return IMG_EXPERIMENT;
		}
		return null;
	}
}
