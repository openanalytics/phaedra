package eu.openanalytics.phaedra.ui.protocol.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

import eu.openanalytics.phaedra.base.ui.colormethod.ColorMethodRegistry;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.norm.NormalizationException;
import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.NormalizationUtils;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class ColorMethodFactory {

	public final static String SETTING_METHOD_ID = "method.id";

	public static String getColorMethodId(Feature feature) {
		String id = ColorMethodRegistry.getInstance().getDefaultColorMethod().getId();
		if (feature != null && feature.getColorMethodSettings() != null
				&& feature.getColorMethodSettings().containsKey(SETTING_METHOD_ID))
			id = feature.getColorMethodSettings().get(SETTING_METHOD_ID);
		return id;
	}

	public static IColorMethod createColorMethod(Feature feature) {
		return createColorMethod(feature, true);
	}

	public static IColorMethod createColorMethod(Feature feature, boolean allowPersonal) {
		if (feature == null) return null;

		IColorMethod method = null;
		if (allowPersonal) {
			method = PersonalColorMethodFactory.getColorMethod(feature);
			if (method != null) return method;
		}

		String id = getColorMethodId(feature);
		if (id == null) return null;

		method = ColorMethodRegistry.getInstance().createMethod(id);
		if (method == null) return null;

		method.configure(feature.getColorMethodSettings());
		return method;
	}

	public static IColorMethodData createData(PlateDataAccessor dataAccessor, Feature feature, String normalization) {
		List<Plate> plates = Lists.newArrayList(dataAccessor.getPlate());
		return createData(plates, feature, normalization, "mean");
	}

	public static IColorMethodData createData(List<Plate> plates, Feature feature, String normalization, String stat) {
		if (feature != null && feature.isNumeric()) {
			int nrOfWells = plates.get(0).getWells().size();
			int nrOfPlates = plates.size();

			List<Double> values = new ArrayList<>();

			for (int i = 0; i < nrOfWells; i++) {
				double[] wellValues = new double[nrOfPlates];
				int validCount = 0;

				for (int j = 0; j < nrOfPlates; j++) {
					Plate plate = plates.get(j);
					if (plate.getWells().size() > i) {
						Well well = PlateUtils.getWell(plate, i+1);
						if (well.getStatus() >= 0) {
							double v = CalculationService.getInstance().getAccessor(plate).getNumericValue(well, feature, normalization);
							if (!Double.isNaN(v)) wellValues[validCount++] = v;
						}
					}
				}

				double[] validValues = new double[validCount];
				System.arraycopy(wellValues, 0, validValues, 0, validCount);

				// If all wells at a particular position are rejected, skip it.
				if (validCount > 0) values.add(StatService.getInstance().calculate(stat, validValues));
			}

			double[] valueArray = Doubles.toArray(values);
			double lc = calculateLCStat(stat, plates, feature, normalization);
			double hc = calculateHCStat(stat, plates, feature, normalization);

			return new SimpleColorMethodData(
					valueArray
					, StatService.getInstance().calculate("min", valueArray)
					, StatService.getInstance().calculate("max", valueArray)
					, StatService.getInstance().calculate("mean", valueArray)
					, lc
					, hc
					);
		}

		return null;
	}

	public static IColorMethodData createData(Experiment exp, Feature feature, String normalization) {
		// Use only the valid (approved) plates
		List<Plate> plates = new ArrayList<>();
		for (Plate p : PlateService.getInstance().getPlates(exp)) {
			if (p.getValidationStatus() >= 0) {
				plates.add(p);
			}
		}
		if (feature != null && feature.isNumeric() && !plates.isEmpty()) {
			int nrOfWells = plates.get(0).getWells().size();
			int nrOfPlates = plates.size();

			List<Double> values = new ArrayList<>();

			for (int i = 0; i < nrOfWells; i++) {
				for (int j = 0; j < nrOfPlates; j++) {
					Plate plate = plates.get(j);
					if (plate.getWells().size() > i) {
						Well well = PlateUtils.getWell(plate, i+1);
						if (well.getStatus() >= 0) {
							double v = CalculationService.getInstance().getAccessor(plate).getNumericValue(well, feature, normalization);
							values.add(v);
						}
					}
				}
			}

			double[] valueArray = Doubles.toArray(values);
			double lc = calculateLCStat("mean", plates, feature, normalization);
			double hc = calculateHCStat("mean", plates, feature, normalization);

			return new SimpleColorMethodData(
					valueArray
					, StatService.getInstance().calculate("min", valueArray)
					, StatService.getInstance().calculate("max", valueArray)
					, StatService.getInstance().calculate("mean", valueArray)
					, lc
					, hc
					);
		}

		return null;
	}

	public static IColorMethodData createData(List<Experiment> exps, Feature feature, String normalization, String stat, boolean isMultiPlate) {
		// Use only the valid (approved) plates
		List<Plate> plates = new ArrayList<>();
		for (Experiment exp : exps) {
			for (Plate p : PlateService.getInstance().getPlates(exp)) {
				if (p.getValidationStatus() >= 0) {
					plates.add(p);
				}
			}
		}
		if (feature != null && feature.isNumeric() && !plates.isEmpty()) {
			int nrOfWells = plates.get(0).getWells().size();
			int nrOfPlates = plates.size();

			List<Double> values = new ArrayList<>();

			for (int i = 0; i < nrOfWells; i++) {
 				double[] wellValues = new double[nrOfPlates];
				int validCount = 0;

				for (int j = 0; j < nrOfPlates; j++) {
					Plate plate = plates.get(j);
					if (plate.getWells().size() > i) {
						Well well = PlateUtils.getWell(plate, i+1);
						if (well.getStatus() >= 0) {
							double v = CalculationService.getInstance().getAccessor(plate).getNumericValue(well, feature, normalization);
							if (isMultiPlate) {
								if (!Double.isNaN(v)) wellValues[validCount++] = v;
							} else {
								values.add(v);
							}
						}
					}
				}

				if (isMultiPlate) {
					double[] validValues = new double[validCount];
					System.arraycopy(wellValues, 0, validValues, 0, validCount);

					// If all wells at a particular position are rejected, skip it.
					if (validCount > 0) values.add(StatService.getInstance().calculate(stat, validValues));
				}
			}

			double[] valueArray = Doubles.toArray(values);
			double lc = calculateLCStat(stat, plates, feature, normalization);
			double hc = calculateHCStat(stat, plates, feature, normalization);

			return new SimpleColorMethodData(
					valueArray
					, StatService.getInstance().calculate("min", valueArray)
					, StatService.getInstance().calculate("max", valueArray)
					, StatService.getInstance().calculate("mean", valueArray)
					, lc
					, hc
					);
		}

		return null;
	}

	private static double calculateLCStat(String stat, List<Plate> plates, Feature feature, String normalization) {
		double[] lcs = new double[plates.size()];
		int i = 0;
		for (Plate plate : plates) {
			// Calculate the median per plate
			NormalizationKey key = new NormalizationKey(plate, feature, normalization);
			try {
				lcs[i] = NormalizationUtils.getLowStat("median", key);
			} catch (NormalizationException e) {
				lcs[i] = Double.NaN;
			}
			i++;
		}
		// Calculate the requested stat across the plate means
		return StatService.getInstance().calculate(stat, lcs);
	}

	private static double calculateHCStat(String stat, List<Plate> plates, Feature feature, String normalization) {
		double[] hcs = new double[plates.size()];
		int i = 0;
		for (Plate plate : plates) {
			// Calculate the median per plate
			NormalizationKey key = new NormalizationKey(plate, feature, normalization);
			try {
				hcs[i] = NormalizationUtils.getHighStat("median", key);
			} catch (NormalizationException e) {
				hcs[i] = Double.NaN;
			}
			i++;
		}
		// Calculate the requested stat across the plate means
		return StatService.getInstance().calculate(stat, hcs);
	}

	public static class SimpleColorMethodData implements IColorMethodData, Serializable {

		private static final long serialVersionUID = -8265541184258058481L;
		
		private double[] values;
		private double min;
		private double max;
		private double mean;
		private double lc;
		private double hc;
		private Double[] percentileCache;

		public SimpleColorMethodData(double[] values, double min, double max, double mean, double lc, double hc) {
			this.values = values;
			this.min = min;
			this.max = max;
			this.mean = mean;
			this.lc = lc;
			this.hc = hc;
			this.percentileCache = new Double[100];
		}

		@Override
		public double getMin() {
			return min;
		}

		@Override
		public double getMax() {
			return max;
		}

		@Override
		public double getMean() {
			return mean;
		}

		@Override
		public double getValue(String name) {
			if (name.equalsIgnoreCase("lc")) {
				return lc;
			} else if (name.equalsIgnoreCase("hc")) {
				return hc;
			} else if (name.startsWith("pct")) {
				int pct = Integer.parseInt(name.substring(3));
				if (pct < 1 || pct > 100) return Double.NaN;
				if (percentileCache[pct-1] == null) percentileCache[pct-1] = StatService.getInstance().calculate("percentile", values, (double)pct);
				return percentileCache[pct-1];
			}
			return 0;
		}

	}
}
