package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class ImageBlinker {

	/**
	 * Blink the image for given label # number of times.
	 *
	 * @param label The label for which the image is blinked.
	 * @param imgName1 Default Image.
	 * @param imgName2 Blink Image.
	 * @param nrOfTimes Number of times to blink the image.
	 */
	public static void blinkImage(Label label, String imgName1, String imgName2, int nrOfTimes) {
		Display.getDefault().timerExec(300, () -> {
			if (nrOfTimes < 0 || label.isDisposed()) return;
			String icon = nrOfTimes % 2 == 0 ? imgName1 : imgName2;
			label.setImage(IconManager.getIconImage(icon));
			blinkImage(label, imgName1, imgName2, nrOfTimes-1);
		});
	}

}
