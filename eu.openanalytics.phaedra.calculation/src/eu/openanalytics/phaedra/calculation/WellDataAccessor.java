package eu.openanalytics.phaedra.calculation;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * TODO
 * Reuses PlateDataAccessor, which means data is loaded per-plate.
 * This is suboptimal for 'scattered' wells, i.e. lists of wells that are spread across many plates.
 */
public class WellDataAccessor implements Serializable {

	private static final long serialVersionUID = -6263540655322756451L;

	public static void fetchFeatureValues(List<Well> wells, Feature feature, @Deprecated boolean checkCacheBeforeQuery) {
		PlateService.streamableList(wells).stream().map(Well::getPlate).distinct().parallel()
			.forEach(plate -> {
				CalculationService.getInstance().getAccessor(plate).loadEager(Collections.singletonList(feature));
			});
	}

	public static void fetchFeatureValues(Well well, List<Feature> features, @Deprecated boolean checkCacheBeforeQuery) {
		CalculationService.getInstance().getAccessor(well.getPlate()).loadEager(features);
	}

	public static void fetchFeatureValues(List<Well> wells, List<Feature> features, boolean checkCacheBeforeQuery, IProgressMonitor monitor) {
		monitor.beginTask("Loading Feature Values for " + wells.size() + " wells (" + features.size() + " features)", 5 + wells.size() * features.size());
		PlateService.streamableList(wells).stream().map(Well::getPlate).distinct().parallel()
		.forEach(plate -> {
			if (monitor.isCanceled()) return;
			CalculationService.getInstance().getAccessor(plate).loadEager(features);
		});
	}

	public static boolean isFeatureValueCached(Well well, Feature feature) {
		return CalculationService.getInstance().getAccessor(well.getPlate()).isDataCached(feature);
	}

}
