package eu.openanalytics.phaedra.model.curve.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.calculation.stat.StatQuery;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.calculation.stat.ctx.StatContextFactory;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;

public class CurveUtils {
	
	public static boolean hasCurve(Feature feature) {
		return CurveFitService.getInstance().getSettings(feature) != null;
	}
	
	public static Predicate<Feature> FEATURES_WITH_CURVES = f -> {
		return hasCurve(f);
	};

	public static double[] calculateBounds(Curve curve) {
		Feature f = curve.getFeature();
		String n = f.getNormalization();
		String lowType = ProtocolUtils.getLowType(curve.getFeature());
		String highType = ProtocolUtils.getHighType(curve.getFeature());
		
		double[] bounds = new double[2];
		
		if (curve.getCompounds().size() > 1) {
			// Use a stat context which automatically takes care of rejected wells, NaNs, etc.
			List<double[]> lowValues = new ArrayList<>();
			List<double[]> highValues = new ArrayList<>();
			for (Compound c: curve.getCompounds()) {
				if (CompoundValidationStatus.INVALIDATED.matches(c)) continue;
				if (PlateValidationStatus.INVALIDATED.matches(c.getPlate())) continue;
				lowValues.add(StatContextFactory.createContext(new StatQuery(null, c.getPlate(), f, lowType, n)).getData(0));
				highValues.add(StatContextFactory.createContext(new StatQuery(null, c.getPlate(), f, highType, n)).getData(0));
			}
			double[] allLowValues = CollectionUtils.merge(lowValues);
			double[] allHighValues = CollectionUtils.merge(highValues);
			bounds[0] = StatService.getInstance().calculate("median", allLowValues);
			bounds[1] = StatService.getInstance().calculate("median", allHighValues);
		} else {
			Plate plate = curve.getCompounds().get(0).getPlate();
			bounds[0] = StatService.getInstance().calculate("median", plate, f, lowType, n);
			bounds[1] = StatService.getInstance().calculate("median", plate, f, highType, n);
		}
		
		Arrays.sort(bounds);
		return bounds;
	}
}
