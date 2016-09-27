package eu.openanalytics.phaedra.ui.curve.details;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.RGB;
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
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.curve.CurveFitInput;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.render.ICurveRenderer;
import eu.openanalytics.phaedra.model.curve.render.ICurveRenderer.CurveAnnotation;
import eu.openanalytics.phaedra.model.curve.render.ICurveRenderer.CurveBand;
import eu.openanalytics.phaedra.model.curve.render.ICurveRenderer.CurveDomainInterval;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
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
		XYSeries curvePoints = new XYSeries("Curve");
		
		List<XYSeriesCollection> curveBandSeries = new ArrayList<>();
		CurveBand[] curveBands = null;
		
		if (curve != null) {
			CurveFitInput input = CurveFitService.getInstance().getInput(curve);
			ICurveRenderer renderer = CurveFitService.getInstance().getRenderer(curve.getModelId());
			
			double[] weights = renderer.getPointWeights(curve, input);
			for (int i = 0; i < input.getValues().length; i++) {
				double xValue = -input.getConcs()[i];
				double yValue = input.getValues()[i];
				boolean valid = input.getValid()[i];

				if (Double.isInfinite(xValue)) continue;

				CrcChartItem item = new CrcChartItem(xValue, yValue);
				item.setWell(input.getWells().get(i));
				item.setWeight(weights == null ? 1.0d : weights[i]);
				
				if (valid) acceptedPoints.add(item);
				else rejectedPoints.add(item);
			}

			double[][] curveSamples = renderer.getCurveSamples(curve, input);
			if (curveSamples != null) {
				for (int i = 0; i < curveSamples[0].length; i++) {
					curvePoints.add(new XYDataItem(curveSamples[0][i], curveSamples[1][i]));
				}
			}

			CurveAnnotation[] annotations = renderer.getAnnotations(curve, input);
			if (annotations != null) {
				for (CurveAnnotation a: annotations) {
					Stroke dashedLine = new BasicStroke(a.thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[] { 2.0f, 2.0f }, 0.0f);
					XYLineAnnotation annotation = new XYLineAnnotation(a.x1, a.y1, a.x2, a.y2, dashedLine, new Color(a.color));
					plot.addAnnotation(annotation);
				}
			}
			
			curveBands = renderer.getBands(curve, input);
			if (curveBands != null) {
				for (CurveBand b: curveBands) {
					XYSeries lower = new XYSeries("Lower band");
					XYSeries upper = new XYSeries("Upper band");
					for (int i = 0; i < b.lowerX.length; i++) {
						lower.add(new XYDataItem(b.lowerX[i], b.lowerY[i]));
						upper.add(new XYDataItem(b.upperX[i], b.upperY[i]));
					}
					XYSeriesCollection band = new XYSeriesCollection();
					band.addSeries(lower);
					band.addSeries(upper);
					curveBandSeries.add(band);
				}
			}
			
			CurveDomainInterval[] domainIntervals = renderer.getDomainIntervals(curve, input);
			if (domainIntervals != null) {
				for (CurveDomainInterval di: domainIntervals) {
					Marker marker = new IntervalMarker(di.x1, di.x2);
					marker.setAlpha(di.alpha);
					marker.setPaint(new Color(di.color));
					plot.addDomainMarker(marker);
				}
			}
			
			double[] plotRange = renderer.getPlotRange(curve, input);
			if (plotRange != null) {
				rangeAxis.setLowerBound(plotRange[0]);
				rangeAxis.setUpperBound(plotRange[1]);
			}
		}

		XYSeriesCollection dsDataPoints = new XYSeriesCollection();
		dsDataPoints.addSeries(acceptedPoints);
		dsDataPoints.addSeries(rejectedPoints);
		
		XYSeriesCollection dsCurve = new XYSeriesCollection();
		dsCurve.addSeries(curvePoints);

		plot.setDataset(0, dsDataPoints);
		plot.setDataset(1, dsCurve);
		for (int i = 0; i < curveBandSeries.size(); i++) plot.setDataset(2+i, curveBandSeries.get(i));

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

		// Curve
		XYLineAndShapeRenderer curveRenderer = new XYLineAndShapeRenderer(true, false);
		int thickness = prefStore.getInt(Prefs.CRC_CURVE_THICKNESS);
		curveRenderer.setSeriesStroke(0, new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		curveRenderer.setSeriesPaint(0, getColor(Prefs.CRC_CURVE_COLOR));
		curveRenderer.setSeriesShapesVisible(0, false);

		// Confidence bands renderer
		XYDifferenceRenderer[] curveBandRenderers = new XYDifferenceRenderer[curveBandSeries.size()];
		for (int i = 0; i < curveBandRenderers.length; i++) {
			int alpha = (int) (curveBands[i].alpha * 255);
			int c = curveBands[i].color | (alpha << 24);
			Color ciColor = new Color(c, true);
			XYDifferenceRenderer ciRenderer = new XYDifferenceRenderer(ciColor, ciColor, false);
			ciColor = new Color(c, false);
			ciRenderer.setSeriesPaint(0, ciColor);
			ciRenderer.setSeriesPaint(1, ciColor);
			curveBandRenderers[i] = ciRenderer;
		}
		
		plot.setRenderer(0, pointRenderer);
		plot.setRenderer(1, curveRenderer);
		for (int i = 0; i < curveBandRenderers.length; i++) plot.setRenderer(i+2, curveBandRenderers[i]);
		
		JFreeChart chart = new JFreeChart(plot);
		chart.removeLegend();
		return chart;
	}

	private static Color getColor(String prefName) {
		RGB rgb = ColorUtils.parseRGBString(prefStore.getString(prefName));
		if (rgb != null) return new Color(rgb.red, rgb.green, rgb.blue);
		else return Color.BLACK;
	}
}
