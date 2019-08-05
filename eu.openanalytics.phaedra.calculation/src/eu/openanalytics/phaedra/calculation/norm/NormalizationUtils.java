package eu.openanalytics.phaedra.calculation.norm;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.stat.StatQuery;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
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
	
	public static double getSamplesStat(String stat, NormalizationKey key) throws NormalizationException {
		Plate plate = SelectionUtils.getAsClass(key.getDataToNormalize(), Plate.class);
		StatQuery query = new StatQuery(stat, plate, key.getFeature(), WellType.SAMPLE, null);
		return StatService.getInstance().calculate(query);
	}
	
}
