package eu.openanalytics.phaedra.model.curve.render;

import eu.openanalytics.phaedra.model.curve.Activator;
import eu.openanalytics.phaedra.model.curve.CurveFitInput;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

public interface ICurveRenderer {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".curveRenderer";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_MODEL_ID = "modelId";
	
	public double[][] getCurveSamples(Curve curve, CurveFitInput input);
	
	public double[] getPointWeights(Curve curve, CurveFitInput input);
	
	public double[] getPlotRange(Curve curve, CurveFitInput input);
	
	public CurveAnnotation[] getAnnotations(Curve curve, CurveFitInput input);
	
	public CurveBand[] getBands(Curve curve, CurveFitInput input);
	
	public CurveDomainInterval[] getDomainIntervals(Curve curve, CurveFitInput input);
	
	public static class CurveAnnotation {
		public float thickness;
		public int color;
		public double x1, y1;
		public double x2, y2;
		
		public CurveAnnotation(float thickness, int color, double x1, double y1, double x2, double y2) {
			this.thickness = thickness;
			this.color = color;
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
	}
	
	public static class CurveBand {
		public int color;
		public float alpha;
		public double[] lowerX, lowerY;
		public double[] upperX, upperY;
		
		public CurveBand(int color, float alpha, double[] lowerX, double[] lowerY, double[] upperX, double[] upperY) {
			this.color = color;
			this.alpha = alpha;
			this.lowerX = lowerX;
			this.lowerY = lowerY;
			this.upperX = upperX;
			this.upperY = upperY;
		}
	}
	
	public static class CurveDomainInterval {
		public int color;
		public float alpha;
		public double x1, x2;
		
		public CurveDomainInterval(int color, float alpha, double x1, double x2) {
			this.color = color;
			this.alpha = alpha;
			this.x1 = x1;
			this.x2 = x2;
		}
	}
}
