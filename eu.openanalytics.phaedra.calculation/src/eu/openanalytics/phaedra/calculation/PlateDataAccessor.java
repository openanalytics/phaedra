package eu.openanalytics.phaedra.calculation;

import static eu.openanalytics.phaedra.calculation.WellDataAccessor.CACHE;
import static eu.openanalytics.phaedra.calculation.WellDataAccessor.MISSING_VALUE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.threading.ThreadUtils;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationTrigger;
import eu.openanalytics.phaedra.calculation.WellDataAccessor.CacheableFeatureValue;
import eu.openanalytics.phaedra.calculation.norm.NormalizationException;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * <p>
 * Accessor for feature values on a per-plate basis.
 * For access on a per-well basis, use {@link WellDataAccessor} instead.
 * </p><p>
 * This accessor will load data in multi-threaded mode if {@link #loadEager} is used
 * and the data size (FeatureCount * WellCount) is sufficiently large.
 * </p>
 * Note: instances of this class should always be obtained via {@link CalculationService#getAccessor(Plate)}.
 */
public class PlateDataAccessor implements Serializable {

	private static final long serialVersionUID = 1233271408452769100L;

	private Plate plate;
	private List<Well> wells;

	private ReentrantLock lock;

	public PlateDataAccessor(Plate plate) {
		this.plate = plate;

		// Keep a List of wells that 1. is sorted by wellNr and 2. can be safely used in streams.
		this.wells = new ArrayList<>(plate.getWells());
		Collections.sort(wells, PlateUtils.WELL_NR_SORTER);

		this.lock = new ReentrantLock();
	}

	public void loadEager(List<Feature> features) {
		if (features == null) features = ProtocolUtils.getFeatures(plate);
		loadData(features);
	}

	public void reset() {
		reset(true);
	}

	public void reset(boolean allFeatures) {
		lock.lock();
		try {
			if (allFeatures) {
				wells.forEach(w -> uncacheValues(w, null));
			} else {
				// Only calculated features
				List<Feature> features = ProtocolUtils.getProtocolClass(plate).getFeatures();
				for (Feature f: features) {
					if (f.isCalculated()) wells.forEach(w -> uncacheValues(w, f));
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public void forceFeatureCalculation(Feature f) {
		if (f == null || !f.isCalculated()) return;
		// Do not lock yet! runCalculatedFeature may call getValue (via JEP) in other threads.
		List<FeatureValue> featureValues = CalculationService.getInstance().runCalculatedFeature(f, plate);
		lock.lock();
		try {
			cacheValues(f, featureValues);
		} finally {
			lock.unlock();
		}
	}

	public Plate getPlate() {
		return plate;
	}

	public String getStringValue(int row, int col, Feature f) {
		int wellNr = NumberUtils.getWellNr(row, col, plate.getColumns());
		return getStringValue(wellNr, f);
	}

	public String getStringValue(int wellNr, Feature f) {
		if (wellNr > wells.size() || f.isNumeric()) return null;
		Well well = wells.get(wellNr-1);
		return getStringValue(well, f);
	}

	public String getStringValue(Well well, Feature f) {
		CacheableFeatureValue value = getValue(well, f);
		if (value == null || value == MISSING_VALUE) return null;

		return value.getRawStringValue();
	}

	public double getNumericValue(int row, int col, Feature f, String normalization) {
		int wellNr = NumberUtils.getWellNr(row, col, plate.getColumns());
		return getNumericValue(wellNr, f, normalization);
	}

	public double getNumericValue(int wellNr, Feature f, String normalization) {
		if (wellNr > wells.size() || !f.isNumeric()) return Double.NaN;
		Well well = wells.get(wellNr-1);
		return getNumericValue(well, f, normalization);
	}

	public double getNumericValue(Well well, Feature f, String normalization) {
		CacheableFeatureValue value = getValue(well, f);
		if (value == null || value == MISSING_VALUE) return Double.NaN;

		double rawValue = value.getRawNumericValue();
		if (normalization == null || normalization.equals(NormalizationService.NORMALIZATION_NONE)) return rawValue;

		// Get an ad-hoc normalization. Because value.getNormalizedValue() may be stale.
		try {
			return NormalizationService.getInstance().getNormalizedValue(plate, f, normalization, well.getRow(), well.getColumn());
		} catch (NormalizationException e) {
			return Double.NaN;
		}
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private CacheableFeatureValue getValue(Well w, Feature f) {
		CacheableFeatureValue value = getCachedValue(w, f);
		if (value == null) {
			lock.lock();
			try {
				// Try again, another thread may have loaded the value while we were waiting.
				value = getCachedValue(w, f);
				if (value == null) {
					// Load the value from the database.
					loadData(Arrays.asList(f));
					// Now value is guaranteed to be not null. It's either a valid value, or MISSING_VALUE.
					value = getCachedValue(w, f);
				}
			} finally {
				lock.unlock();
			}
		}
		return value;
	}

	private void loadData(List<Feature> features) {
		if (features == null || features.isEmpty()) return;
		lock.lock();
		try {
			if (features.size() == 1) {
				fetchValues(features);
			} else {
				List<Feature> uncachedFeatures = new ArrayList<>();
				for (Feature f : features) {
					if (!isFullyCached(f)) uncachedFeatures.add(f);
				}
				if (uncachedFeatures.isEmpty()) return;

				int featureCount = uncachedFeatures.size();
				int items = featureCount * plate.getWells().size();
				int splits;
				if (items < 3000) splits = 1;
				else if (items < 16000) splits = 2;
				else if (items < 24000) splits = 3;
				else splits = 4;
				int splitSize = (featureCount + splits) / splits;

				if (splits < 2) {
					// Load group single-threaded.
					fetchValues(uncachedFeatures);
				} else {
					// Load subgroups multi-threaded.
					ThreadUtils.runQuery(() -> {
						IntStream.range(0, splits).parallel().forEach(i -> {
							int fromIndex = i*splitSize;
							int toIndex = Math.min(fromIndex + splitSize, uncachedFeatures.size());
							List<Feature> subList = uncachedFeatures.subList(fromIndex, toIndex);
							fetchValues(subList);
						});
					});
				}
			}
		} finally {
			lock.unlock();
		}
	}

	private void fetchValues(List<Feature> features) {
		List<FeatureValue> allValues = null;
		if (features.size() == PlateUtils.getFeatures(plate).size()) {
			allValues = PlateService.getInstance().getWellData(plate);
		} else {
			allValues = PlateService.getInstance().getWellData(plate, features);
		}

		// Group the values by feature
		Map<Feature, List<FeatureValue>> map = new HashMap<>();
		for (FeatureValue value : allValues) {
			Feature f = value.getFeature();
			List<FeatureValue> featureValues = map.get(f);
			if (featureValues == null) {
				featureValues = new ArrayList<FeatureValue>();
				map.put(f, featureValues);
			}
			featureValues.add(value);
		}

		// Cache the values
		for (Feature f : features) {
			List<FeatureValue> featureValues = map.get(f);
			if ((featureValues == null || featureValues.isEmpty()) && CalculationTrigger.PlateRecalc.matches(f)) {
				// Still no values for a calculated feature?
				// This must be the first time ever the feature is accessed. Calculate (and save) now.
				// Note: runCalculatedFeature may cause parallel calls to getValue, so the lock must be released during the call!
				int lockCount = lock.getHoldCount();
				try {
					for (int i = 0; i < lockCount; i++) lock.unlock();
					featureValues = CalculationService.getInstance().runCalculatedFeature(f, plate);
				} finally {
					for (int i = 0; i < lockCount; i++) lock.lock();
				}
			}
			cacheValues(f, featureValues);
		}
	}

	private boolean isFullyCached(Feature f) {
		Well uncachedWell = wells.stream().filter(w -> getCachedValue(w, f) == null).findAny().orElse(null);
		return (uncachedWell == null);
	}

	private void cacheValues(Feature f, List<FeatureValue> values) {
		if (values == null) {
			wells.forEach(w -> getCachedMap(w, true).put(f.getId(), MISSING_VALUE));
			return;
		}
		values.forEach(fv -> {
			Map<Long, CacheableFeatureValue> map = getCachedMap(fv.getWell(), true);
			map.put(f.getId(), new CacheableFeatureValue(fv));
			//TODO Refresh cache entry (for size calculation and disk flushing), but this is very slow:
//			CACHE.put(getCacheKey(fv.getWell()), map);
		});
	}

	private void uncacheValues(Well well, Feature f) {
		if (f == null) {
			// Remove entire map
			CACHE.remove(getCacheKey(well));
		} else {
			// Remove one feature from map
			Map<Long, CacheableFeatureValue> featureValueMap = getCachedMap(well, false);
			if (featureValueMap != null) featureValueMap.remove(f.getId());
		}
	}

	private CacheableFeatureValue getCachedValue(Well well, Feature feature) {
		Map<Long, CacheableFeatureValue> featureValueMap = getCachedMap(well, false);
		if (featureValueMap != null) return featureValueMap.get(feature.getId());
		return null;
	}

	@SuppressWarnings("unchecked")
	private Map<Long, CacheableFeatureValue> getCachedMap(Well well, boolean createIfMissing) {
		Object cacheKey = getCacheKey(well);
		Map<Long, CacheableFeatureValue> featureValueMap = (Map<Long, CacheableFeatureValue>) CACHE.get(cacheKey);
		if (featureValueMap == null && createIfMissing) {
			featureValueMap = new HashMap<>();
			CACHE.put(cacheKey, featureValueMap);
		}
		return featureValueMap;
	}

	private static Object getCacheKey(Well well) {
		return well.getId();
	}
}
