package eu.openanalytics.phaedra.ui.curve.details;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.RGB;
import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.fit.CurveDataPoints;
import eu.openanalytics.phaedra.model.curve.util.CurvePredictor;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.OSBCurve;
import eu.openanalytics.phaedra.model.curve.vo.PLACCurve;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.curve.Activator;
import eu.openanalytics.phaedra.ui.curve.prefs.Prefs;

public class CrcChartFactory {

	public final static String STROKE_FULL = "Full";
	public final static String STROKE_DASHED = "Dash";

	private static IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();

	public static JFreeChart createChart(Curve curve) {

		XYPlot plot = new XYPlot();
		if (curve == null) return new JFreeChart(plot);
		
		String domain = "Log[M]";
		NumberAxis domainAxis = new NumberAxis(domain);
		domainAxis.setAutoRange(true);
		domainAxis.setAutoRangeIncludesZero(false);
		plot.setDomainAxis(domainAxis);
		
		String range = curve.getFeature().getName() + " [" + curve.getFeature().getNormalization() + "]";
		NumberAxis rangeAxis = new NumberAxis(range);
		plot.setRangeAxis(rangeAxis);

		XYSeries acceptedPoints = new XYSeries("Accepted");
		XYSeries rejectedPoints = new XYSeries("Rejected");
		XYSeries upperBound = new XYSeries("Upper");
		XYSeries lowerBound = new XYSeries("Lower");
		XYSeries curvePoints = new XYSeries("Curve");
		XYSeries upperConfBand = new XYSeries("Upper CI band");
		XYSeries lowerConfBand = new XYSeries("Lower CI band");

		//TODO This list (and fitData below) is empty if the compound or plate is invalidated.
		List<Well> wells = CurveService.getInstance().getSampleWells(curve);

		if (!wells.isEmpty() && curve != null) {
			CurveDataPoints fitData = CurveService.getInstance().getDataPoints(curve);
			double upper = getUpperBound(curve);
			double lower = getLowerBound(curve);

			double rangeBoundMin = lower;
			double rangeBoundMax = upper;

			// Note: the order of points in fitData is the same order as compound.getWells()
			int i=0;
			for (Well well: wells) {
				double xValue = -fitData.concs[i];
				double yValue = fitData.values[i];
				int accept = fitData.accepts[i];
				i++;

				if (Double.isInfinite(xValue)) continue;

				rangeBoundMin = Math.min(rangeBoundMin, yValue);
				rangeBoundMax = Math.max(rangeBoundMax, yValue);

				CrcChartItem item = new CrcChartItem(xValue, yValue);
				item.setWell(well);
				item.setWeight(1.0d);
				if (curve instanceof OSBCurve && ((OSBCurve)curve).getWeights() != null) {
					double[] weights = ((OSBCurve)curve).getWeights();
					item.setWeight(weights[i-1]);
				}
				
				if (accept >= 0) acceptedPoints.add(item);
				else rejectedPoints.add(item);
				upperBound.add(new XYDataItem(xValue, upper));
				lowerBound.add(new XYDataItem(xValue, lower));
			}

			// Adjust range axis for plots with very small heights
			if (rangeBoundMax - rangeBoundMin < 1) {
				double diff = rangeBoundMax - rangeBoundMin;
				rangeAxis.setLowerBound(rangeBoundMin - diff/2);
				rangeAxis.setUpperBound(rangeBoundMax + diff/2);
			}

			double[][] v = CurvePredictor.predict(curve, 30);
			for (i = 0; i < v[0].length; i++) curvePoints.add(new XYDataItem(v[0][i], v[1][i]));

			// OSB-specific markers
			if (curve instanceof OSBCurve && v[0].length > 0) {
				OSBCurve osb = (OSBCurve)curve;
				double[] pic50 = new double[] { osb.getPic50(), osb.getPic50StdErr() };
				double[] pic20 = new double[] { osb.getPic20(), Double.NaN };
				double[] pic80 = new double[] { osb.getPic80(), Double.NaN };
				
				Stroke dashedLine = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[] { 2.0f, 2.0f }, 0.0f);
				
				if (prefStore.getBoolean(Prefs.CRC_SHOW_PIC50_MARKER) && pic50 != null) {
					double pic50Y = lower + (upper - lower) / 2;
					XYLineAnnotation ann = new XYLineAnnotation(-pic50[0], Integer.MIN_VALUE, -pic50[0], pic50Y, dashedLine, ChartColor.BLACK);
					plot.addAnnotation(ann);
					ann = new XYLineAnnotation(Integer.MIN_VALUE, pic50Y, -pic50[0], pic50Y, dashedLine, ChartColor.BLACK);
					plot.addAnnotation(ann);
				}
				
				if (prefStore.getBoolean(Prefs.CRC_SHOW_OTHER_IC_MARKERS) && pic20 != null) {
					double pic20Y = lower + (upper - lower) * 0.2;
					XYLineAnnotation ann = new XYLineAnnotation(-pic20[0], Integer.MIN_VALUE, -pic20[0], pic20Y, dashedLine, ChartColor.BLACK);
					plot.addAnnotation(ann);
					ann = new XYLineAnnotation(Integer.MIN_VALUE, pic20Y, -pic20[0], pic20Y, dashedLine, ChartColor.BLACK);
					plot.addAnnotation(ann);
				}
				
				if (prefStore.getBoolean(Prefs.CRC_SHOW_OTHER_IC_MARKERS) && pic80 != null) {
					double pic80Y = lower + (upper - lower) * 0.8;
					XYLineAnnotation ann = new XYLineAnnotation(-pic80[0], Integer.MIN_VALUE, -pic80[0], pic80Y, dashedLine, ChartColor.BLACK);
					plot.addAnnotation(ann);
					ann = new XYLineAnnotation(Integer.MIN_VALUE, pic80Y, -pic80[0], pic80Y, dashedLine, ChartColor.BLACK);
					plot.addAnnotation(ann);
				}

				if (prefStore.getBoolean(Prefs.CRC_SHOW_CONF_AREA)) {
					double[][] ciGrid = osb.getCiGrid();
					
					if (ciGrid != null) {
						// Confidence band
						for (i=0; i<ciGrid[0].length; i++) lowerConfBand.add(new XYDataItem(ciGrid[0][i]/Math.log(10), ciGrid[1][i]));
						for (i=0; i<ciGrid[0].length; i++) upperConfBand.add(new XYDataItem(ciGrid[0][i]/Math.log(10), ciGrid[2][i]));
					} else if (pic50 != null) {
						// Confidence interval
						Marker marker = new IntervalMarker(-pic50[0] - (3.0 * pic50[1]), -pic50[0] + (3.0 * pic50[1]));
						float alpha = (float) prefStore.getInt(Prefs.CRC_CONF_AREA_ALPHA) / 100;
						marker.setAlpha(alpha);
						RGB rgb = ColorUtils.parseRGBString(prefStore.getString(Prefs.CRC_CONF_AREA_COLOR));
						marker.setPaint(new Color(rgb.red, rgb.green, rgb.blue));
						plot.addDomainMarker(marker);
					}
				}
			}

			// pLAC-specific markers
			if (curve instanceof PLACCurve) {
				PLACCurve plac = (PLACCurve)curve;
				double threshold = plac.getThreshold();
				
				if (!Double.isNaN(threshold)) {
					Stroke dashedLine = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[] { 6.0f, 6.0f }, 0.0f);
					XYLineAnnotation ann = new XYLineAnnotation(Integer.MIN_VALUE, threshold, Integer.MAX_VALUE, threshold, dashedLine, ChartColor.BLUE);
					plot.addAnnotation(ann);
					if (threshold > 0) rangeAxis.setAutoRangeMinimumSize(2.4*threshold);
				}
			}
		}

		XYSeriesCollection dsDataPoints = new XYSeriesCollection();
		dsDataPoints.addSeries(acceptedPoints);
		dsDataPoints.addSeries(rejectedPoints);
		dsDataPoints.addSeries(upperBound);
		dsDataPoints.addSeries(lowerBound);
		
		XYSeriesCollection dsCIBand = new XYSeriesCollection();
		dsCIBand.addSeries(lowerConfBand);
		dsCIBand.addSeries(upperConfBand);
		
		XYSeriesCollection dsCurve = new XYSeriesCollection();
		dsCurve.addSeries(curvePoints);

		plot.setDataset(0, dsDataPoints);
		plot.setDataset(1, dsCIBand);
		plot.setDataset(2, dsCurve);

		final int dataPointSize = prefStore.getInt(Prefs.CRC_POINT_SIZE);
		
		XYLineAndShapeRenderer pointRenderer = new MultiSelectRenderer(false, true) {
			private static final long serialVersionUID = 9136997925956594125L;

			@Override
			protected void drawSecondaryPass(Graphics2D g2, XYPlot plot, XYDataset dataset, int pass, int series, int item,
                    ValueAxis domainAxis, Rectangle2D dataArea, ValueAxis rangeAxis, CrosshairState crosshairState, EntityCollection entities) {
				
				if (series == 0) {
					CrcChartItem cItem = (CrcChartItem) ((XYSeriesCollection)dataset).getSeries(series).getDataItem(item);
					int x = (int) plot.getDomainAxis().valueToJava2D(cItem.getXValue(), dataArea, plot.getDomainAxisEdge());
					int y = (int) plot.getRangeAxis().valueToJava2D(cItem.getYValue(), dataArea, plot.getRangeAxisEdge());
					
					double weight = 0.3 + (cItem.getWeight() * 0.7);
					int weightedSize = Math.max(1, (int) Math.round(weight * dataPointSize));
					Shape shape = new Ellipse2D.Float(x-weightedSize, y-weightedSize, weightedSize * 2, weightedSize * 2);
					
					g2.setStroke(new BasicStroke());
					g2.setColor(getColor(Prefs.CRC_POINT_COLOR_ACCEPTED));
					if (prefStore.getBoolean(Prefs.CRC_SHOW_WEIGHTS)) g2.drawString(NumberUtils.round(cItem.getWeight(), 2), x+15, y);
					g2.draw(shape);
					if (getItemShapeFilled(series, item)) g2.fill(shape);
					
					addEntity(entities, shape, dataset, series, item, x, y);
				}
				else super.drawSecondaryPass(g2, plot, dataset, pass, series, item, domainAxis, dataArea, rangeAxis, crosshairState, entities);
			}
		};
		pointRenderer.setDrawSeriesLineAsPath(true);
		
		// Accepted wells
		Shape acceptShape = new Ellipse2D.Float(-dataPointSize, -dataPointSize, dataPointSize * 2, dataPointSize * 2);
		pointRenderer.setSeriesShape(0, acceptShape);
		pointRenderer.setSeriesPaint(0, getColor(Prefs.CRC_POINT_COLOR_ACCEPTED));

		// Rejected wells
		double sizeArmLength = Math.sqrt((Math.pow(dataPointSize, 2)) / 2);
		double sizeThickness = sizeArmLength / 3;
		Shape rejectShape = ShapeUtilities.createDiagonalCross((float) sizeArmLength, (float) sizeThickness);
		pointRenderer.setSeriesShape(1, rejectShape);
		pointRenderer.setSeriesPaint(1, getColor(Prefs.CRC_POINT_COLOR_REJECTED));

		// Upper bound
		float thickness = prefStore.getInt(Prefs.CRC_BOUND_THICKNESS);
		pointRenderer.setSeriesLinesVisible(2, true);
		pointRenderer.setSeriesShapesVisible(2, false);
		pointRenderer.setSeriesStroke(2, new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[] { 10.0f, 10.0f }, 0.0f));
		pointRenderer.setSeriesPaint(2, getColor(Prefs.CRC_BOUND_COLOR_UPPER));

		// Lower bound
		pointRenderer.setSeriesLinesVisible(3, true);
		pointRenderer.setSeriesShapesVisible(3, false);
		pointRenderer.setSeriesStroke(3, new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[] { 10.0f, 10.0f }, 0.0f));
		pointRenderer.setSeriesPaint(3, getColor(Prefs.CRC_BOUND_COLOR_LOWER));

		// Confidence bands renderer
		RGB rgb = ColorUtils.parseRGBString(prefStore.getString(Prefs.CRC_CONF_AREA_COLOR));
		int ciAlpha = (int)(2.55 * prefStore.getInt(Prefs.CRC_CONF_AREA_ALPHA));
		Color ciColor = new Color(rgb.red, rgb.green, rgb.blue, ciAlpha);
		XYDifferenceRenderer ciRenderer = new XYDifferenceRenderer(ciColor, ciColor, false);
		ciColor = new Color(rgb.red, rgb.green, rgb.blue);
		ciRenderer.setSeriesPaint(0, ciColor);
		ciRenderer.setSeriesPaint(1, ciColor);

		// Curve
		XYLineAndShapeRenderer curveRenderer = null;
		if (curve != null && "SPLINE".equals(curve.getSettings().getMethod())) {
			curveRenderer = new XYSplineRenderer();
			((XYSplineRenderer)curveRenderer).setPrecision(10);
		} else {
			curveRenderer = new XYLineAndShapeRenderer(true, false);
		}
		thickness = prefStore.getInt(Prefs.CRC_CURVE_THICKNESS);
		curveRenderer.setSeriesStroke(0, new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		curveRenderer.setSeriesPaint(0, getColor(Prefs.CRC_CURVE_COLOR));
		curveRenderer.setSeriesShapesVisible(0, false);

		plot.setRenderer(0, pointRenderer);
		plot.setRenderer(1, ciRenderer);
		plot.setRenderer(2, curveRenderer);

		JFreeChart chart = new JFreeChart(plot);
		chart.removeLegend();
		return chart;
	}

	private static Color getColor(String prefName) {
		RGB rgb = ColorUtils.parseRGBString(prefStore.getString(prefName));
		if (rgb != null) {
			return new Color(rgb.red, rgb.green, rgb.blue);
		}
		return Color.BLACK;
	}

	private static double getUpperBound(Curve curve) {
		if (curve instanceof OSBCurve) {
			return ((OSBCurve)curve).getUb();
		} else {
			return CurveService.getInstance().getDataPoints(curve).hcMean;
		}
	}

	private static double getLowerBound(Curve curve) {
		if (curve instanceof OSBCurve) {
			return ((OSBCurve)curve).getLb();
		} else {
			return CurveService.getInstance().getDataPoints(curve).lcMean;
		}
	}
}
