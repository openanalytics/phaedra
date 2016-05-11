package eu.openanalytics.phaedra.base.ui.charting.v2.util;

import uk.ac.starlink.ttools.plot.Plot3D;

public class TopcatViewStyles {
	
	public final static String DEFAULT = "default";

	public final static String X_UP = "x up";
	public final static String Y_UP = "y up";
	public final static String Z_UP = "z up";
	
	public final static String XY_VIEW = "xy";
	public final static String NEG_XY_VIEW = "-xy";
	public final static String ZY_VIEW = "zy";
	public final static String NEG_ZY_VIEW = "- zy";
	public final static String XZ_VIEW = "xz";
	public final static String NEG_XZ_VIEW = "- xz";
	
	public final static String[] STYLES = new String[]{
		DEFAULT,
		X_UP,
		Y_UP,
		Z_UP,
		XY_VIEW,
		NEG_XY_VIEW,
		ZY_VIEW,
		NEG_ZY_VIEW,
		XZ_VIEW,
		NEG_XZ_VIEW
	};
	
	private final static double[] rotBase = new double[] {
		1, 0, 0,
		0, 1, 0,
		0, 0, -1 };

	private final static double[] defaultView =
			Plot3D.rotateXY(
					Plot3D.rotateXY(rotBase, 0.5, 0.5 * Math.PI ),
			0, -0.1 * Math.PI );
	
	private final static double[] xUp = 
			Plot3D.rotateXY(
					Plot3D.rotateXY(
							Plot3D.rotateXY(rotBase, -Math.PI/2  , 0),
					0, -Math.PI/2),
			0.2, -0.2);
	private final static double[] yUp = Plot3D.rotateXY(rotBase, 0.2 , -0.2 );
	private final static double[] zUp =
			Plot3D.rotateXY(
					Plot3D.rotateXY(
							Plot3D.rotateXY(rotBase, 0 , Math.PI / 2 ),
					Math.PI/2 ,0),
			0.2, -0.2);
	
	private final static double[] xyView = Plot3D.rotateXY(rotBase, 0, 0);
	private final static double[] negXYView = Plot3D.rotateXY(rotBase, Math.PI, 0);
	
	private final static double[] zyView = Plot3D.rotateXY(rotBase, -Math.PI/2, 0);
	private final static double[] negZYView = Plot3D.rotateXY(rotBase, Math.PI/2 , 0 );
	
	private final static double[] xzView = Plot3D.rotateXY(rotBase, 0, Math.PI/2);
	private final static double[] negXZView = Plot3D.rotateXY(rotBase, Math.PI, Math.PI/2);
	
	public final static double[] getView(String style){
		switch(style){
		case DEFAULT:
			return defaultView;
		case X_UP:
			return xUp;
		case Y_UP:
			return yUp;
		case Z_UP:
			return zUp;
		case XY_VIEW : 
			return xyView;
		case NEG_XY_VIEW:
			return negXYView;
		case ZY_VIEW:
			return zyView;
		case NEG_ZY_VIEW:
			return negZYView;
		case XZ_VIEW:
			return xzView;
		case NEG_XZ_VIEW:
			return negXZView;
		default : 
			return defaultView;
		}
	}
	
}
