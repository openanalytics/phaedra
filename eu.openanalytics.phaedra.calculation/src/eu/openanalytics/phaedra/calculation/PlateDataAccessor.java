package eu.openanalytics.phaedra.calculation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationTrigger;
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
 * Accessor for well feature values on a per-plate basis.
 * </p>
 * <p>
 * Under normal circumstances, data will be lazily loaded and cached per-feature.
 * To load multiple features immediately, use {@link #loadEager}.
 * </p>
 * Note: instances of this class should always be obtained via {@link CalculationService#getAccessor(Plate)}.
 */
public class PlateDataAccessor implements Serializable {

	private static final long serialVersionUID = 1233271408452769100L;

	private Plate plate;
	private List<Well> wells;

	private ReentrantLock lock;

	private static final ICache CACHE = CacheService.getInstance().createCache("WellDataCache");
	
	public PlateDataAccessor(Plate plate) {
		this.plate = plate;

		// Keep a List of wells that 1. is sorted by wellNr and 2. can be safely used in streams.
		this.wells = new ArrayList<>(plate.getWells());
		Collections.sort(wells, PlateUtils.WELL_NR_SORTER);

		this.lock = new ReentrantLock();
	}

	/**
	 * Immediately load the data for the given well features.
	 * 
	 * @param features The features to load.
	 */
	public void loadEager(List<Feature> features) {
		if (features == null) features = ProtocolUtils.getFeatures(plate);
		loadData(features);
	}

	/**
	 * See {@link PlateDataAccessor#reset(boolean)}
	 */
	public void reset() {
		reset(true);
	}

	/**
	 * Clear data from the cache.
	 * 
	 * @param allFeatures True to clear all data, false to clear only data for calculated features.
	 */
	public void reset(boolean allFeatures) {
		lock.lock();
		try {
			List<Feature> features = ProtocolUtils.getProtocolClass(plate).getFeatures();
			for (Feature f: features) {
				if (allFeatures || f.isCalculated()) removeFromCache(f);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Force calculation of a calculated feature, bypassing any cached values.
	 * <p>
	 * Normally, calculated features are calculated lazily, when they are requested and not yet cached.
	 * </p> 
	 * @param f The feature to calculate.
	 */
	public void forceFeatureCalculation(Feature f) {
		if (f == null || !f.isCalculated()) return;
		// Do not lock yet! runCalculatedFeature may call getValue (via JEP) in other threads.
		List<FeatureValue> featureValues = CalculationService.getInstance().runCalculatedFeature(f, plate);
		lock.lock();
		try {
			addDataToCache(f, featureValues);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Get the plate this accessor is for.
	 * 
	 * @return The parent plate.
	 */
	public Plate getPlate() {
		return plate;
	}

	/**
	 * Get the string value of the given feature at the given well position.
	 * 
	 * @param row The well row, starting at 1.
	 * @param col The well column, starting at 1.
	 * @param f The well feature.
	 * @return The string value, possibly null.
	 */
	public String getStringValue(int row, int col, Feature f) {
		int wellNr = NumberUtils.getWellNr(row, col, plate.getColumns());
		return getStringValue(wellNr, f);
	}

	/**
	 * Get the string value of the given feature and the given well number.
	 * 
	 * @param wellNr The well number, starting at 1.
	 * @param f The well feature.
	 * @return The string value, possibly null.
	 */
	public String getStringValue(int wellNr, Feature f) {
		if (wellNr > wells.size() || f.isNumeric()) return null;
		Well well = wells.get(wellNr-1);
		return getStringValue(well, f);
	}

	/**
	 * Get the string value of the given feature and well.
	 * 
	 * @param well The well to get a value for.
	 * @param f The well feature.
	 * @return The string value, possibly null.
	 */
	public String getStringValue(Well well, Feature f) {
		if (f.isNumeric()) return null;
		
		String[] data = (String[]) getCachedData(f);
		if (data == null) return null;
		
		int wellNr = PlateUtils.getWellNr(well);
		return data[wellNr - 1];
	}

	/**
	 * Get the numeric value of the given feature at the given well position.
	 * 
	 * @param row The well row, starting at 1.
	 * @param col The well column, starting at 1.
	 * @param f The well feature.
	 * @param normalization To get a normalized value, specify the normalization method here. See {@link NormalizationService}
	 * @return The numeric value, possibly NaN (not-a-number).
	 */
	public double getNumericValue(int row, int col, Feature f, String normalization) {
		int wellNr = NumberUtils.getWellNr(row, col, plate.getColumns());
		return getNumericValue(wellNr, f, normalization);
	}

	/**
	 * Get the numeric value of the given feature at the given well number.
	 * 
	 * @param wellNr The well number, starting at 1.
	 * @param f The well feature.
	 * @param normalization To get a normalized value, specify the normalization method here. See {@link NormalizationService}
	 * @return The numeric value, possibly NaN (not-a-number).
	 */
	public double getNumericValue(int wellNr, Feature f, String normalization) {
		if (wellNr > wells.size() || !f.isNumeric()) return Double.NaN;
		Well well = wells.get(wellNr-1);
		return getNumericValue(well, f, normalization);
	}

	/**
	 * Get the numeric value of the given feature and well.
	 * 
	 * @param well The well to get a value for.
	 * @param f The well feature.
	 * @param normalization To get a normalized value, specify the normalization method here. See {@link NormalizationService}
	 * @return The numeric value, possibly NaN (not-a-number).
	 */
	public double getNumericValue(Well well, Feature f, String normalization) {
		if (!f.isNumeric()) return Double.NaN;
		
		if (normalization == null || normalization.equals(NormalizationService.NORMALIZATION_NONE)) {
			double[] data = (double[]) getCachedData(f);
			if (data == null) {
				lock.lock();
				try {
					// Try again, another thread may have loaded the value while we were waiting.
					data = (double[]) getCachedData(f);
					if (data == null) loadData(Collections.singletonList(f));
				} finally {
					lock.unlock();
				}
				data = (double[]) getCachedData(f);
			}
			if (data == null) return Double.NaN;
			
			int wellNr = PlateUtils.getWellNr(well);
			return data[wellNr - 1];
		} else {
			// Get an ad-hoc normalization. This class does not cache normalized values.
			try {
				return NormalizationService.getInstance().getNormalizedValue(plate, f, normalization, well.getRow(), well.getColumn());
			} catch (NormalizationException e) {
				return Double.NaN;
			}	
		}
	}
	
	public boolean isDataCached(Feature feature) {
		return isCached(feature);
	}

	private void loadData(List<Feature> features) {
		if (features == null || features.isEmpty()) return;
		lock.lock();
		try {
			List<Feature> uncachedFeatures = new ArrayList<>();
			for (Feature f : features) {
				if (!isCached(f)) uncachedFeatures.add(f);
			}
			if (uncachedFeatures.isEmpty()) return;
			fetchValues(uncachedFeatures);
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
			addDataToCache(f, featureValues);
		}
	}

	/**
	 * *******
	 * Caching
	 * *******
	 */
	
	private Object getCacheKey(Feature feature) {
		return CacheKey.create(plate.getId(), feature.getId());
	}
	
	private boolean isCached(Feature feature) {
		if (feature == null) return false;
		Object key = getCacheKey(feature);
		return CACHE.contains(key);
	}
	
	private Object getCachedData(Feature feature) {
		if (feature == null) return null;
		Object key = getCacheKey(feature);
		return CACHE.get(key);
	}
	
	private void addDataToCache(Feature feature, List<FeatureValue> featureValues) {
		if (feature == null) return;
		if (featureValues == null || featureValues.isEmpty()) {
			addDataValuesToCache(feature, null);
		} else {
			int wellCount = PlateUtils.getWellCount(plate);
			if (feature.isNumeric()) {
				double[] data = new double[wellCount];
				for (FeatureValue fv: featureValues) {
					int wellNr = PlateUtils.getWellNr(fv.getWell());
					data[wellNr - 1] = fv.getRawNumericValue();
				}
				addDataValuesToCache(feature, data);
			} else {
				String[] data = new String[wellCount];
				for (FeatureValue fv: featureValues) {
					int wellNr = PlateUtils.getWellNr(fv.getWell());
					data[wellNr - 1] = fv.getRawStringValue();
				}
				addDataValuesToCache(feature, data);
			}
		}
	}
	
	private void addDataValuesToCache(Feature feature, Object data) {
		if (feature == null) return;
		Object key = getCacheKey(feature);
		Object dataToCache = data;
		if (dataToCache == null) {
			int wellCount = PlateUtils.getWellCount(plate);
			if (feature.isNumeric()) {
				dataToCache = new double[wellCount];
				Arrays.fill((double[]) dataToCache, Double.NaN);
			} else {
				dataToCache = new String[wellCount];
			}
		}
		CACHE.put(key, dataToCache);
	}
	
	private void removeFromCache(Feature feature) {
		if (feature == null) return;
		Object key = getCacheKey(feature);
		CACHE.remove(key);
	}
}
