package eu.openanalytics.phaedra.model.curve.util;

import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.plate.vo.Compound;

public class ValidationFlagProvider {

	private static Image statusOkImg = IconManager.getIconImage("flag_green.png");
	private static Image statusNOkImg = IconManager.getIconImage("flag_red.png");
	private static Image statusNotSetImg = IconManager.getIconImage("flag_white.png");
	private static Image statusNotNeededImg = IconManager.getIconImage("flag_blue.png");
	
	public static Image getValidationImage(Compound compound) {
		int status = compound.getValidationStatus();		
		return getValidationImage(status);
	}
	
	public static Image getValidationImage(int status) {
		if (status > 1) return statusOkImg;
		else if (status == 1) return statusNotNeededImg;
		else if (status < 0) return statusNOkImg;
		else return statusNotSetImg;
	}
}
