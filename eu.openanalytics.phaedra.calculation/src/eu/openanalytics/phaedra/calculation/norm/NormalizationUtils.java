package eu.openanalytics.phaedra.calculation.norm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.stat.StatQuery;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;

public class NormalizationUtils {

	public static double getLowStat(String stat, NormalizationKey key) throws NormalizationException {
		String type = "LC";
		if (key.getFeature() instanceof Feature) type = ProtocolUtils.getLowType((Feature)key.getFeature());
		if (type == null) throw new NormalizationException("No Low Control type configured");
		return getControlsStat(stat, type, key);
	}
	
	public static double getHighStat(String stat, NormalizationKey key) throws NormalizationException {
		String type = "HC";
		if (key.getFeature() instanceof Feature) type = ProtocolUtils.getHighType((Feature)key.getFeature());
		if (type == null) throw new NormalizationException("No High Control type configured");
		return getControlsStat(stat, type, key);
	}
	
	private static double getControlsStat(String stat, String wellType, NormalizationKey key) {
		Plate plate = SelectionUtils.getAsClass(key.getDataToNormalize(), Plate.class);
		StatQuery query = new StatQuery(stat, plate, key.getFeature(), wellType, null);
		double result = StatService.getInstance().calculate(query);
		if (Double.isNaN(result)) throw new NormalizationException("No valid controls of type " + wellType);
		return result;
	}
	
	public static double getSamplesLowStat(String stat, NormalizationKey key) throws NormalizationException {
		Feature feature = (Feature) key.getFeature();
		String lowType = ProtocolUtils.getLowType(feature);
		Plate plate = SelectionUtils.getAsClass(key.getDataToNormalize(), Plate.class);
		List<Well> wells = new ArrayList<>();
		wells.addAll(plate.getWells());
		
		List<Well> validWells = wells.stream()
				.filter(w -> w.getStatus() >= 0)
				.filter(w -> WellType.SAMPLE.equals(w.getWellType()) || lowType.equals(w.getWellType()))
				.collect(Collectors.toList());
		
		ToDoubleFunction<Well> valueGetter = w -> CalculationService.getInstance().getAccessor(w.getPlate()).getNumericValue(w, feature, null);
		double[] values = validWells.stream().mapToDouble(valueGetter).filter(v -> (!Double.isNaN(v))).toArray();
		return StatService.getInstance().calculate(stat, values);
	}

	//PHA-674: Robust Z-score on samples (only)
	public static double getSamplesStat(String stat, NormalizationKey key) {
		Feature feature = (Feature) key.getFeature();
		Plate plate = SelectionUtils.getAsClass(key.getDataToNormalize(), Plate.class);
		List<Well> wells = new ArrayList<>();
		wells.addAll(plate.getWells());
		
		List<Well> validWells = wells.stream()
				.filter(w -> w.getStatus() >= 0)
				.filter(w -> WellType.SAMPLE.equals(w.getWellType()))
				.collect(Collectors.toList());
		
		ToDoubleFunction<Well> valueGetter = w -> CalculationService.getInstance().getAccessor(w.getPlate()).getNumericValue(w, feature, null);
		double[] values = validWells.stream().mapToDouble(valueGetter).filter(v -> (!Double.isNaN(v))).toArray();
		return StatService.getInstance().calculate(stat, values);
	}
	
}
