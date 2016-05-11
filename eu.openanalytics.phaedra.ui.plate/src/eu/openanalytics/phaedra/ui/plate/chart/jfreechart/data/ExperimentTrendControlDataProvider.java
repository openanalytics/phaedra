package eu.openanalytics.phaedra.ui.plate.chart.jfreechart.data;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import eu.openanalytics.phaedra.base.ui.charting.data.IDataProviderWPrintSupport;
import eu.openanalytics.phaedra.base.ui.charting.render.IRenderCustomizer;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;

public class ExperimentTrendControlDataProvider extends IDataProviderWPrintSupport<Plate> {

	private List<Plate> plates = null;
	private List<String> wellTypes = null;
	private List<String> visibleControls;

	private Feature feature = null;

	public ExperimentTrendControlDataProvider(List<Plate> plates, Feature feature) {
		this.plates = plates;
		this.feature = feature;
		this.wellTypes = getWelltypes();
		this.visibleControls = new ArrayList<>(wellTypes);
	}

	@Override
	public int getSeriesCount() {
		return wellTypes.size();
	}

	@Override
	public String getSeriesName(int seriesIndex) {
		return wellTypes.get(seriesIndex);
	}

	@Override
	public List<Plate> buildSeries(int seriesIndex) {
		return plates;
	}

	@Override
	public String[] getParameters() {
		return null;
	}

	@Override
	public Map<String, List<String>> getGroupedFeatures() {
		return null;
	}

	@Override
	public double[] getValue(Plate item, String[] parameters, int row) {
		String wellType = parameters[0];
		if (feature == null) feature = ProtocolUIService.getInstance().getCurrentFeature();

		double mean = StatService.getInstance().calculate("mean", item, feature, wellType, null);
		double stdev = StatService.getInstance().calculate("stdev", item, feature, wellType, null);

		double[] value = new double[3];
		value[0] = mean;
		value[1] = mean - (3 * stdev);
		value[2] = mean + (3 * stdev);
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
		if (dimension == 0)
			axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		if (dimension == 1) {
			if (feature == null)
				feature = ProtocolUIService.getInstance().getCurrentFeature();
			String label = feature.getDisplayName() + " [raw]";
			axis.setLabel(label);
		}
		return axis;
	}

	@Override
	public IRenderCustomizer createRenderCustomizer() {
		return new RenderCustomizer();
	}

	public void setVisibleControls(List<String> visibleControls) {
		this.visibleControls = visibleControls;
	}

	public class RenderCustomizer implements IRenderCustomizer {
		@Override
		public void customize(XYItemRenderer renderer) {
			int visibleIndex = 0;
			for (int i = 0; i < getSeriesCount(); i++) {
				String seriesName = getSeriesName(i);
				if (visibleControls.contains(seriesName)) {
					renderer.setSeriesStroke(visibleIndex, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					RGB rgb = ProtocolUtils.getWellTypeRGB(seriesName);
					Color color = new Color(rgb.red, rgb.green, rgb.blue);
					renderer.setSeriesPaint(visibleIndex, color);
					((AbstractRenderer) renderer).setSeriesFillPaint(visibleIndex, color);
					visibleIndex++;
				}
			}
		}
	}

	private List<String> getWelltypes() {
		List<String> welltypes = new ArrayList<String>();
		String low = ProtocolUtils.getLowType(feature);
		String high = ProtocolUtils.getHighType(feature);
		if (low != null)
			welltypes.add(low);
		if (high != null)
			welltypes.add(high);
		welltypes.add("SAMPLE");
		welltypes.add("EMPTY");
		return welltypes;
	}
}