package eu.openanalytics.phaedra.ui.plate.chart.jfreechart.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.axis.NumberAxis;

import eu.openanalytics.phaedra.base.ui.charting.data.IDataProvider;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;

public class WellSpiderDataProvider implements IDataProvider<Well> {

	private Well well;
	private List<String> series;

	public WellSpiderDataProvider(Well well) {
		this.well = well;

		this.series = new ArrayList<String>();
		series.add(WellType.LC); //PHA-644
		if (well.getCompound() != null) {
			series.add(well.getCompound().toString());
		} else {
			series.add(NumberUtils.getWellCoordinate(well.getRow(), well.getColumn())
					+ " (" + well.getWellType() + ")");
		}
		series.add(WellType.HC); //PHA-644
	}

	@Override
	public int getSeriesCount() {
		return series.size();
	}

	@Override
	public String getSeriesName(int seriesIndex) {
		return series.get(seriesIndex);
	}

	@Override
	public List<Well> buildSeries(int seriesIndex) {
		List<Well> wells = new ArrayList<Well>();
		wells.add(well);
		return wells;
	}

	@Override
	public String[] getParameters() {
		List<Feature> features = PlateUtils.getFeatures(well.getPlate());
		features = CollectionUtils.findAll(features, ProtocolUtils.KEY_FEATURES);
		features = CollectionUtils.findAll(features, ProtocolUtils.NUMERIC_FEATURES);
		String[] featureNames = CollectionUtils.transformToStringArray(features, ProtocolUtils.FEATURE_NAMES);
		return featureNames;
	}

	@Override
	public Map<String, List<String>> getGroupedFeatures() {
		Map<String, List<String>> groupedFeatures = new HashMap<>();
		List<Feature> features = PlateUtils.getFeatures(well.getPlate());
		features = CollectionUtils.findAll(features, ProtocolUtils.KEY_FEATURES);
		features = CollectionUtils.findAll(features, ProtocolUtils.NUMERIC_FEATURES);

		for (Feature f : features) {
			String group = f.getFeatureGroup() != null ? f.getFeatureGroup().getName() : null;
			if (!groupedFeatures.containsKey(group)) {
				groupedFeatures.put(group, new ArrayList<String>());
			}
			groupedFeatures.get(group).add(f.getDisplayName());
		}

		return groupedFeatures;
	}

	@Override
	public double[] getValue(Well item, String[] parameters, int row) {
		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(item.getPlate());
		String seriesName = parameters[0];
		double[] values = new double[parameters.length];
		for (int i=1; i<parameters.length; i++) {
			String featureName = parameters[i];

			ProtocolClass pClass = item.getPlate().getExperiment().getProtocol().getProtocolClass();
			Feature f = ProtocolUtils.getFeatureByName(featureName, pClass);
			String normalization = f.getNormalization();

			// Allow a custom normalization via the active feature selection.
			Feature activeFeature = ProtocolUIService.getInstance().getCurrentFeature();
			if (f.equals(activeFeature)) normalization = ProtocolUIService.getInstance().getCurrentNormalization();

			if (seriesName.equals("LC") || seriesName.equals("HC")) {
				WellType wellType = ProtocolService.getInstance().getWellTypeByCode(seriesName).orElse(null);
				values[i] = StatService.getInstance().calculate("mean", item.getPlate(), f, wellType, normalization);
			} else {
				values[i] = accessor.getNumericValue(item, f, normalization);
			}
		}
		return values;
	}

	@Override
	public String getLabel(Well well) {
		return null;
	}

	@Override
	public NumberAxis createAxis(int dimension, String parameter) {
		return null;
	}

	@Override
	public double[] getGlobalMinMax(String[] parameters) {
		return null;
	}

}