package eu.openanalytics.phaedra.ui.plate.chart.jfreechart.data;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.RGB;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.AbstractRenderer;

import eu.openanalytics.phaedra.base.ui.charting.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.render.ICategoryRenderCustomizer;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;

public class PlateBoxDataProvider implements IDataProvider<Plate> {

	private List<Plate> plates;
	private List<String> wellTypes;
	private List<String> plateLabels;

	public PlateBoxDataProvider(List<Plate> plates) {
		this.plates = plates;
		this.wellTypes = getUniqueWellTypes(plates);

		this.plateLabels = new ArrayList<>();
		for (int i = 0; i < plates.size(); i++) {
			Plate plate = plates.get(i);
			String lbl = "Exp" + plate.getExperiment().getId() + "-p" + plate.getSequence();
			int counter = 1;
			while (plateLabels.contains(lbl)) {
				lbl = "Exp" + plate.getExperiment().getId() + "-p" + plate.getSequence() + " (" + counter++ + ")";
			}
			plateLabels.add(lbl);
		}
		
		Collections.sort(wellTypes, (o1, o2) -> {
			if (o1 == null) return -1;
			if (o2 == null) return 1;
			WellType t1 = ProtocolService.getInstance().getWellTypeByCode(o1).orElse(null);
			WellType t2 = ProtocolService.getInstance().getWellTypeByCode(o2).orElse(null);
			if (t1 == null && t2 == null) return 0;
			if (t1 == null) return -1;
			if (t2 == null) return 1;
			return t1.compareTo(t2);
		});
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

		if (item.getExperiment().getProtocol().getProtocolClass() != ProtocolUIService.getInstance().getCurrentProtocolClass())
			ProtocolUIService.getInstance().setCurrentProtocolClass(item.getExperiment().getProtocol().getProtocolClass());

		Feature f = ProtocolUIService.getInstance().getCurrentFeature();
		WellType wellType = ProtocolService.getInstance().getWellTypeByCode(parameters[0]).orElse(null);
		
		double mean = StatService.getInstance().calculate("mean", item, f, wellType, f.getNormalization());
		if (Double.isNaN(mean)) return null;
		double median = StatService.getInstance().calculate("median", item, f, wellType, f.getNormalization());
		if (Double.isNaN(median)) return null;
		double stdev = StatService.getInstance().calculate("stdev", item, f, wellType, f.getNormalization());
		if (Double.isNaN(stdev)) return null;
		double min = StatService.getInstance().calculate("min", item, f, wellType, f.getNormalization());
		if (Double.isNaN(min)) return null;
		double max = StatService.getInstance().calculate("max", item, f, wellType, f.getNormalization());
		if (Double.isNaN(max)) return null;

		double[] value = new double[8];
		value[0] = mean;
		value[1] = median;
		value[2] = mean - stdev;
		value[3] = mean + stdev;
		value[4] = min;
		value[5] = max;
		value[6] = min;
		value[7] = max;

		return value;
	}

	@Override
	public String getLabel(Plate plate) {
		return plateLabels.get(plates.indexOf(plate));
	}

	@Override
	public NumberAxis createAxis(int dimension, String parameter) {
		NumberAxis axis = new NumberAxis();
		axis.setAutoRange(true);
		if (dimension == 0) axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		return axis;
	}

	public ICategoryRenderCustomizer createRenderCustomizer() {
		return renderer -> {
			for (int i = 0; i < getSeriesCount(); i++) {
				renderer.setSeriesStroke(i, new BasicStroke(getLineWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				RGB rgb = ProtocolUtils.getWellTypeRGB(getSeriesName(i));
				Color color = new Color(rgb.red, rgb.green, rgb.blue);
				renderer.setSeriesPaint(i, color);
				((AbstractRenderer)renderer).setSeriesFillPaint(i, color);
			}
		};
	}

	private float getLineWidth() {
		if (plates.size() < 10) {
			return 3f;
		} else if (plates.size() < 20) {
			return 2f;
		}
		return 1f;
	}

	@Override
	public double[] getGlobalMinMax(String[] parameters) {
		return null;
	}

	private List<String> getUniqueWellTypes(List<Plate> plates) {
		Set<String> tempWellTypes = new HashSet<>();
		for (Plate plate: plates) {
			for (Well well: plate.getWells()) {
				String wellType = well.getWellType();
				if (wellType != null) tempWellTypes.add(wellType);
			}
		}
		return new ArrayList<>(tempWellTypes);
	}

}