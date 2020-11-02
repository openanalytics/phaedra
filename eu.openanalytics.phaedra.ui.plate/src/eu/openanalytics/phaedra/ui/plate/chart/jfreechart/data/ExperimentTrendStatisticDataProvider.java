package eu.openanalytics.phaedra.ui.plate.chart.jfreechart.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import eu.openanalytics.phaedra.base.ui.charting.data.IDataProviderWPrintSupport;
import eu.openanalytics.phaedra.base.ui.charting.render.IRenderCustomizer;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;

public class ExperimentTrendStatisticDataProvider extends IDataProviderWPrintSupport<Plate> {

	private List<Plate> plates;
	private String selectedStatistic;

	private Feature feature = null;

	public ExperimentTrendStatisticDataProvider(List<Plate> plates, Feature feature) {
		this.plates = plates;
		this.selectedStatistic = "Z-Prime";
		this.feature = feature;
	}

	@Override
	public int getSeriesCount() {
		return 1;
	}

	@Override
	public String getSeriesName(int seriesIndex) {
		return selectedStatistic;
	}

	@Override
	public List<Plate> buildSeries(int seriesIndex) {
		return plates;
	}

	@Override
	public String[] getParameters() {
		List<String> statistics = new ArrayList<String>();

		statistics.add("Z-Prime");
		statistics.add("Signal/Noise");
		statistics.add("Signal/Background");

		if (!plates.isEmpty()) {
			List<String> wellTypes = PlateUtils.getWellTypes(plates.get(0));
			Collections.sort(wellTypes);
			for (String type : wellTypes) {
				statistics.add("%CV for " + type);
			}
		}

		statistics.add("None");

		String[] parameters = statistics.toArray(new String[statistics.size()]);
		return parameters;
	}

	@Override
	public Map<String, List<String>> getGroupedFeatures() {
		return null;
	}

	@Override
	public double[] getValue(Plate item, String[] parameters, int row) {

		if (feature == null) feature = ProtocolUIService.getInstance().getCurrentFeature();

		double[] value = new double[1];
		value[0] = Double.NaN;

		selectedStatistic = parameters[0];
		if (selectedStatistic == null)
			return value;

		if (selectedStatistic.equals("None"))
			return value;

		String wellTypeCode = "";
		if (selectedStatistic.startsWith("%CV")) {
			wellTypeCode = selectedStatistic.substring(selectedStatistic.indexOf(" for ") + 5);
			selectedStatistic = "%CV";
		}

		double stat = Double.NaN;
		switch (selectedStatistic) {
		case "Z-Prime":
			stat = StatService.getInstance().calculate("zprime", item, feature, null, null);
			break;
		case "Signal/Noise":
			stat = StatService.getInstance().calculate("sn", item, feature, null, null);
			break;
		case "Signal/Background":
			stat = StatService.getInstance().calculate("sb", item, feature, null, null);
			break;
		case "%CV":
			WellType wellType = ProtocolService.getInstance().getWellTypeByCode(wellTypeCode).orElse(null);
			stat = StatService.getInstance().calculate("cv", item, feature, wellType, null);
			break;
		default:
			break;
		}

		value[0] = stat;
		return value;
	}

	@Override
	public String getLabel(Plate plate) {
		return "Exp" + plate.getExperiment().getId() + "-p" + plate.getSequence();
	}


	@Override
	public NumberAxis createAxis(int dimension, String parameter) {
		NumberAxis axis = new NumberAxis();
		axis.setAutoRange(true);
		if (dimension == 0) axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		else axis.setLabel(selectedStatistic);

		axis.setAutoRange(true);
		axis.setAutoRangeIncludesZero(true);
		// axis.setLowerBound(true);
		// axis.setRange(0, 1);
		if (selectedStatistic.equals("Z-Prime")) {
			axis.setAutoRange(false);
			axis.setRange(0, 1);
		}

		return axis;
	}

	@Override
	public IRenderCustomizer createRenderCustomizer() {
		return new RenderCustomizer();
	}

	public class RenderCustomizer implements IRenderCustomizer {
		@Override
		public void customize(XYItemRenderer renderer) {
			XYBarRenderer bar = (XYBarRenderer) renderer;
			bar.setBarPainter(new StandardXYBarPainter());
			bar.setSeriesPaint(0, Color.cyan);
			bar.setShadowVisible(false);
			bar.setDrawBarOutline(false);
			bar.setGradientPaintTransformer(null);
			bar.setMargin(0.2);

		}
	}

}