package eu.openanalytics.phaedra.ui.plate.util;

import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PlateLabelProvider extends ExperimentLabelProvider {
	private static final Image IMG_PROTOCOL_CLASS = IconManager.getIconImage("plate.png");

	@Override
	public String getText(Object element) {
		if (element instanceof Plate) {
			Plate plate = (Plate) element;
			return plate.getSequence() + " - " + plate.getBarcode();
		}

		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof Plate) {
			return IMG_PROTOCOL_CLASS;
		}

		return super.getImage(element);
	}
}
