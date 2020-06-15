package eu.openanalytics.phaedra.calculation.stat.ctx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.stat.StatQuery;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class StatContextFactory {

	public static IStatContext createContext(StatQuery query) {
		if (query.getFeature() instanceof Feature) return createWellFeatureContext(query);
		else if (query.getFeature() instanceof SubWellFeature) return createSubWellFeatureContext(query);
		return null;
	}
	
	private static IStatContext createWellFeatureContext(StatQuery query) {
		// Gather all wells in a List.
		List<Well> wells = new ArrayList<>();
		// UR-015: Container for the duplo wells, if duple plate exists
		List<Well> duploWells = new ArrayList<Well>();
		if (query.getObject() instanceof Well) {
			wells.add((Well)query.getObject());
		} else if (query.getObject() instanceof Plate) {
			Plate plate = (Plate)query.getObject();
			wells.addAll(plate.getWells());
			// UR-015: Test if the plate has a duplo plate, only if a plate has a duplicate the Pearson and Spearman statics can be calculated 
			List<Plate> duploPlates = CalculationService.getInstance().getMultiploPlates(plate);
			if (duploPlates != null && duploPlates.size() == 2) {
				Plate duploPlate = duploPlates.get(1);
				duploWells.addAll(duploPlate.getWells());
			}
		} else if (query.getObject() instanceof Experiment) {
			List<Plate> plates = PlateService.getInstance().getPlates((Experiment)query.getObject());
			for (Plate plate: plates) {
				wells.addAll(plate.getWells());
			}
		}

		Feature feature = (Feature)query.getFeature();
		String norm = ("NONE".equals(query.getNormalization())) ? null : query.getNormalization();

		// Exclude wells as requested.
		List<Well> validWells = wells.stream()
			.filter(w -> query.isIncludeRejected() || w.getStatus() >= 0)
			.filter(w -> query.getWellType() == null || query.getWellType().equals(w.getWellType()))
			.collect(Collectors.toList());
		// UR-015: If duploWells exists it is filtered to validDuploWells list
		List<Well> validDuploWells = duploWells.stream()
				.filter(w -> query.isIncludeRejected() || w.getStatus() >= 0)
				.filter(w -> query.getWellType() == null || query.getWellType().equals(w.getWellType()))
				.collect(Collectors.toList());
		
		ToDoubleFunction<Well> valueGetter = w -> CalculationService.getInstance().getAccessor(w.getPlate()).getNumericValue(w, feature, norm);
		DoublePredicate valueFilter = v -> (!Double.isNaN(v));
		
		String lowType = ProtocolUtils.getLowType(feature);
		String highType = ProtocolUtils.getHighType(feature);
		Predicate<Well> lowFilter = w -> w.getWellType() != null && w.getWellType().equals(lowType);
		Predicate<Well> highFilter = w -> w.getWellType() != null && w.getWellType().equals(highType);
		
		double[] values = validWells.stream().mapToDouble(valueGetter).filter(valueFilter).toArray();
		double[] lows = validWells.stream().filter(lowFilter).mapToDouble(valueGetter).filter(valueFilter).toArray();
		double[] highs = validWells.stream().filter(highFilter).mapToDouble(valueGetter).filter(valueFilter).toArray();
		// // UR-015: Get all values for the duplo plates 
		double[] duploValues = validDuploWells.stream().mapToDouble(valueGetter).filter(valueFilter).toArray();
		
		if (ArrayUtils.isNotEmpty(duploValues)) {
			return new SimpleStatContext(new double[][] { values, lows, highs, duploValues });
		} else {
			return new SimpleStatContext(new double[][] { values, lows, highs });
		}
	}
	
	private static IStatContext createSubWellFeatureContext(StatQuery query) {
		
		ProtocolClass pClass = null;
		
		// Retrieve all wells for the given query object.
		List<Well> wells = new ArrayList<>();
		if (query.getObject() instanceof Well) {
			Well well = (Well)query.getObject();
			pClass = PlateUtils.getProtocolClass(well);
			wells.add(well);
		} else if (query.getObject() instanceof Plate) {
			Plate plate = (Plate)query.getObject();
			pClass = PlateUtils.getProtocolClass(plate);
			wells.addAll(plate.getWells());
		} else if (query.getObject() instanceof Experiment) {
			Experiment exp = (Experiment)query.getObject();
			List<Plate> plates = PlateService.getInstance().getPlates(exp);
			pClass = exp.getProtocol().getProtocolClass();
			for (Plate plate: plates) {
				// Always omit invalidated plates.
				if (plate.getValidationStatus() < 0) continue;
				wells.addAll(plate.getWells());
			}
		}
		
		// If asked, omit rejected wells.
		if (!query.isIncludeRejected()) {
			wells = wells.stream().filter(PlateUtils.ACCEPTED_WELLS_ONLY).collect(Collectors.toList());
		}
		
		// If asked, filter on welltype.
		if (query.getWellType() != null) {
			wells = wells.stream().filter(PlateUtils.createWellTypeFilter(query.getWellType())).collect(Collectors.toList());
		}

		// Create lists of LC and HC values, which is a convenience property of SimpleStatContext.
		String lowType = pClass.getLowWellTypeCode();
		String highType = pClass.getHighWellTypeCode();
		List<Double> lows = new ArrayList<Double>();
		List<Double> highs = new ArrayList<Double>();

		// Gather data for all selected wells.
		int totalSize = 0;
		Map<Well, float[]> dataPerWell = new HashMap<>();
		for (Well well: wells) {
			float[] data = SubWellService.getInstance().getNumericData(well, (SubWellFeature)query.getFeature());
			if (data == null || data.length == 0) continue;
			dataPerWell.put(well, data);
			totalSize += data.length;
		}
		
		// Put all values in a single array, filtering out NaN values.
		double[] totalArray = new double[totalSize];
		int index = 0;
		for (Well well: dataPerWell.keySet()) {
			boolean isLC = lowType.equals(well.getWellType());
			boolean isHC = highType.equals(well.getWellType());
			
			float[] data = dataPerWell.get(well);
			for (int i=0; i<data.length; i++) {
				if (Double.isNaN(data[i])) continue;
				totalArray[index++] = data[i];
				if (isLC) lows.add((double)data[i]);
				if (isHC) highs.add((double)data[i]);
			}
		}

		// All NaNs have been discarded for statistic calculation.
		double[] inputValues = new double[index];
		System.arraycopy(totalArray, 0, inputValues, 0, index);

		IStatContext ctx = new SimpleStatContext(new double[][] { inputValues, CollectionUtils.toArray(lows), CollectionUtils.toArray(highs) });
		
		return ctx;
	}
}
