package eu.openanalytics.phaedra.base.ui.util.gaussianfilter;

public class Translate{

	private double width;
	private double height;
	private double xMinValue;
	private double xMaxValue;
	private double yMinValue;
	private double yMaxValue;

	public Translate(double width, double height, double xMaxValue, double yMaxValue) {
		this(width, height, 0d, xMaxValue, 0d, yMaxValue);
	}

	public Translate(double width, double height, double xMinValue, double xMaxValue, double yMinValue, double yMaxValue) {
		this.width = width;
		this.height = height;
		this.xMinValue = xMinValue;
		this.xMaxValue = xMaxValue;
		this.yMinValue = yMinValue;
		this.yMaxValue = yMaxValue;
	}

	/**
	 * Translates a given domain x coordinate to its corresponding screen x coordinate.
	 */
	public int domainXtoScreenX(double x) {
		return (int) scale(x, xMinValue, xMaxValue, 0, width);
	}

	/**
	 * Translates a given domain y coordinate to its corresponding screen y coordinate.
	 *
	 * Note: Bottom is 0, top is height.
	 */
	public int domainYtoScreenY(double y) {
		return (int) scale(y, yMinValue, yMaxValue, 0, height);
	}

	/**
	 * Translates a given domain y coordinate to its corresponding screen y coordinate.
	 *
	 * Note: Bottom is height, top is 0.
	 */
	public int domainYtoScreenYInverted(double y) {
		return (int) Math.abs(scale(y, yMinValue, yMaxValue, -height, 0));
	}

	/**
	 * Translates a given screen x coordinate to its corresponding domain x coordinate.
	 */
	public double screenXToDomainX(double x) {
		return scale(x, 0, width, xMinValue, xMaxValue);
	}

	/**
	 * Translates a given screen y coordinate to its corresponding domain y coordinate
	 *
	 * Note: Bottom is 0, top is height.
	 */
	public double screenYToDomainY(double y) {
		return scale(y, 0, height, yMinValue, yMaxValue);
	}

	/**
	 * Translates a given screen y coordinate to its corresponding domain y coordinate
	 *
	 * Note: Bottom is height, top is 0.
	 */
	public double screenYToDomainYInverted(double y) {
		return scale(-y, -height, 0, yMinValue, yMaxValue);
	}

	public double getHeight() {
		return height;
	}

	public double getWidth() {
		return width;
	}

	private double scale(double valueIn, double baseMin, double baseMax, double limitMin, double limitMax) {
		return ((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
	}

}