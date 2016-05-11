package eu.openanalytics.phaedra.calculation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class WellDataAccessor implements Serializable {

	private static final long serialVersionUID = -6263540655322756451L;
	private static final int SQL_IN_MAX_ELEMENTS = 1000;

	// Note: only protected because it is shared with PlateDataAcessor.
	protected static final ICache CACHE = CacheService.getInstance().createCache("WellDataCache");
	protected static final CacheableFeatureValue MISSING_VALUE = new CacheableFeatureValue();

	public static void fetchFeatureValues(List<Well> wells, Feature feature, boolean checkCacheBeforeQuery) {
		List<Well> wellsToQuery = Collections.synchronizedList(new ArrayList<>());
		if (checkCacheBeforeQuery) {
			wells.parallelStream().forEach(w -> {
				Object key = getCacheKey(w);
				Map<Long, CacheableFeatureValue> featureValueMap = getCachedValues(key);
				if (featureValueMap.get(feature.getId()) == null) wellsToQuery.add(w);
			});
		} else {
			wellsToQuery.addAll(wells);
		}

		if (!wellsToQuery.isEmpty()) {
			int nrOfWells = wellsToQuery.size();
			if (nrOfWells > SQL_IN_MAX_ELEMENTS) {
				int splits = (nrOfWells + SQL_IN_MAX_ELEMENTS - 1) / SQL_IN_MAX_ELEMENTS;
				ThreadUtils.runQuery(() -> {
					IntStream.range(0, splits).parallel().forEach(i -> {
						int fromIndex = i*SQL_IN_MAX_ELEMENTS;
						int toIndex = Math.min(fromIndex + SQL_IN_MAX_ELEMENTS, nrOfWells);
						List<Well> wellsSubList = wellsToQuery.subList(fromIndex, toIndex);
						List<FeatureValue> newValues = PlateService.getInstance().getWellData(wellsSubList, feature);
						cacheFeatureValues(newValues, wellsSubList, Arrays.asList(feature));
					});
				});
			} else {
				// Get the values that have not been cached yet.
				List<FeatureValue> newValues = PlateService.getInstance().getWellData(wellsToQuery, feature);
				cacheFeatureValues(newValues, wellsToQuery, Arrays.asList(feature));
			}
		}
	}

	public static void fetchFeatureValues(Well well, List<Feature> features, boolean checkCacheBeforeQuery) {
		List<Feature> featuresToQuery = Collections.synchronizedList(new ArrayList<>());
		if (checkCacheBeforeQuery) {
			Object key = getCacheKey(well);
			Map<Long, CacheableFeatureValue> featureValueMap = getCachedValues(key);
			for (Feature f : features) {
				if (!featureValueMap.containsKey(f.getId())) featuresToQuery.add(f);
			}
		} else {
			featuresToQuery.addAll(features);
		}

		if (!featuresToQuery.isEmpty()) {
			int nrOfFeatures = featuresToQuery.size();
			if (nrOfFeatures > SQL_IN_MAX_ELEMENTS) {
				int splits = (nrOfFeatures + SQL_IN_MAX_ELEMENTS - 1) / SQL_IN_MAX_ELEMENTS;
				ThreadUtils.runQuery(() -> {
					IntStream.range(0, splits).parallel().forEach(i -> {
						int fromIndex = i*SQL_IN_MAX_ELEMENTS;
						int toIndex = Math.min(fromIndex + SQL_IN_MAX_ELEMENTS, nrOfFeatures);
						List<Feature> featuresSubList = featuresToQuery.subList(fromIndex, toIndex);
						List<FeatureValue> newValues = PlateService.getInstance().getWellData(well, featuresSubList);
						cacheFeatureValues(newValues, Arrays.asList(well), featuresSubList);
					});
				});
			} else {
				// Get the values that have not been cached yet.
				List<FeatureValue> newValues = PlateService.getInstance().getWellData(well, features);
				cacheFeatureValues(newValues, Arrays.asList(well), features);
			}
		}
	}

	public static void fetchFeatureValues(List<Well> wells, List<Feature> features, boolean checkCacheBeforeQuery, IProgressMonitor monitor) {
		monitor.beginTask("Loading Feature Values for " + wells.size() + " wells (" + features.size() + " features)", 5 + wells.size() * features.size());

		// Get the Wells and Features for which there are no cached values.
		List<Well> wellsToQuery = Collections.synchronizedList(new ArrayList<>());
		List<Feature> featuresToQuery = Collections.synchronizedList(new ArrayList<>());
		if (checkCacheBeforeQuery) {
			// Check which values already have been cached.
			Set<Feature> uncachedFeatures = new HashSet<>();
			Set<Well> uncachedWells = new HashSet<>();
			for (Well w : wells) {
				if (monitor.isCanceled()) return;
				Object key = getCacheKey(w);
				Map<Long, CacheableFeatureValue> featureValueMap = getCachedValues(key);
				for (Feature f : features) {
					CacheableFeatureValue featureValue = featureValueMap.get(f.getId());
					if (featureValue == null) {
						uncachedFeatures.add(f);
						uncachedWells.add(w);
					}
				}
			};
			wellsToQuery.addAll(uncachedWells);
			featuresToQuery.addAll(uncachedFeatures);
		} else {
			wellsToQuery.addAll(wells);
			featuresToQuery.addAll(features);
		}
		// Update progress.
		int featuresToQuerySize = featuresToQuery.size();
		int wellToQuerySize = wellsToQuery.size();
		monitor.worked(5 + ((wells.size() * features.size()) - (wellToQuerySize * featuresToQuerySize)));

		if (!wellsToQuery.isEmpty() && !featuresToQuery.isEmpty()) {
			if (monitor.isCanceled()) return;

			if (featuresToQuerySize > SQL_IN_MAX_ELEMENTS) {
				// The amount of Features exceeds the max amount of SQL IN.
				int featureDivider = (int) Math.ceil(featuresToQuerySize / (double) SQL_IN_MAX_ELEMENTS);
				int subFeatureSize = (int) Math.ceil(featuresToQuerySize / (double) featureDivider);

				for (int i = 0; i < featureDivider; i++) {
					int fromIndex = i*subFeatureSize;
					int toIndex = Math.min(fromIndex + subFeatureSize, featuresToQuerySize);
					List<Feature> subFeaturesToQuery = featuresToQuery.subList(fromIndex, toIndex);
					fetchFeatureValues(wellsToQuery, subFeaturesToQuery, monitor);
				}
			} else {
				// The amount of Features is fine.
				fetchFeatureValues(wellsToQuery, featuresToQuery, monitor);
			}
		}
	}

	public static boolean isFeatureValueCached(Well well, Feature feature) {
		Object key = getCacheKey(well);
		Map<Long, CacheableFeatureValue> cachedValues = getCachedValues(key);
		CacheableFeatureValue value = cachedValues.get(feature.getId());
		return value != null;
	}

	private static void fetchFeatureValues(List<Well> wells, List<Feature> features, IProgressMonitor monitor) {
		int nrOfWells = wells.size();
		int nrOfFeatures = features.size();
		int maxQuerySize = Math.min(SQL_IN_MAX_ELEMENTS, 30_000 / nrOfFeatures);
		if (nrOfWells > maxQuerySize) {
			int splits = (nrOfWells + maxQuerySize - 1) / maxQuerySize;
			int work = maxQuerySize * nrOfFeatures;
			AtomicInteger wellsDone = new AtomicInteger(0);
			ThreadUtils.runQuery(() -> {
				IntStream.range(0, splits).parallel().forEach(i -> {
					if(monitor.isCanceled()) return;
					int fromIndex = i*maxQuerySize;
					int toIndex = Math.min(fromIndex + maxQuerySize, nrOfWells);
					List<Well> wellsSubList = wells.subList(fromIndex, toIndex);
					List<FeatureValue> newValues = PlateService.getInstance().getWellData(wellsSubList, features);
					cacheFeatureValues(newValues, wellsSubList, features);
					monitor.subTask(wellsDone.addAndGet(wellsSubList.size()) + "/" + nrOfWells + " Wells Done.");
					monitor.worked(work);
				});
			});
		} else {
			// Get the values that have not been cached yet.
			List<FeatureValue> newValues = PlateService.getInstance().getWellData(wells, features);
			cacheFeatureValues(newValues, wells, features);
			monitor.worked(100);
		}
	}

	/**
	 * <p>Cache the given list of FeatureValue objects.</p>
	 *
	 * <p>If no FeatureValue is found for the given Wells and Features, a missing value will be cached.</p>
	 *
	 * @param values The FeatureValues to cache
	 * @param wells The Wells for which a FeatureValue is expected
	 * @param features The Features for which a FeatureValue is expected
	 */
	private static void cacheFeatureValues(Collection<FeatureValue> values, List<Well> wells, List<Feature> features) {
		Map<String, CacheableFeatureValue> temp = new HashMap<>();
		for (FeatureValue value : values) temp.put(value.getWellId() + "-" + value.getFeatureId(), new CacheableFeatureValue(value));
		wells.forEach(w -> {
			Object cacheKey = getCacheKey(w);
			Map<Long, CacheableFeatureValue> featureValueMap = getCachedValues(cacheKey);
			for (Feature f : features) {
				CacheableFeatureValue value = temp.get(w.getId() + "-" + f.getId());
				if (value == null) value = MISSING_VALUE;
				featureValueMap.put(f.getId(), value);
			}
			// Replace in cache, to refresh disk cache.
			CACHE.put(cacheKey, featureValueMap);
		});
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, CacheableFeatureValue> getCachedValues(Object cacheKey) {
		Map<Long, CacheableFeatureValue> featureValueMap = (Map<Long, CacheableFeatureValue>) CACHE.get(cacheKey);
		if (featureValueMap == null) CACHE.put(cacheKey, featureValueMap = new HashMap<>());
		return featureValueMap;
	}

	private static Object getCacheKey(Well well) {
		return well.getId();
	}

	/**
	 * A cache-friendly version of FeatureValue:
	 * does not contain any reference to Feature or Well (those are in the key anyway).
	 */
	public static class CacheableFeatureValue implements Serializable {

		private static final long serialVersionUID = 2808394031747386418L;

		private double rawNumericValue;
		private String rawStringValue;

		public CacheableFeatureValue() {
			this.rawNumericValue = Double.NaN;
		}

		public CacheableFeatureValue(FeatureValue fv) {
			rawNumericValue = fv.getRawNumericValue();
			rawStringValue = fv.getRawStringValue();
		}

		public double getRawNumericValue() {
			return rawNumericValue;
		}

		public String getRawStringValue() {
			return rawStringValue;
		}
	}

}
