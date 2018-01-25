package eu.openanalytics.phaedra.model.subwell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.cache.SubWellDataCache;
import eu.openanalytics.phaedra.model.subwell.data.DataSourceFactory;
import eu.openanalytics.phaedra.model.subwell.data.HDF5Datasource;
import eu.openanalytics.phaedra.model.subwell.data.ISubWellDataSource;

/**
 * API for interaction with subwell data. This includes retrieving data and writing or updating data.
 * Subwell data can either be numeric (32bit floating point) or text (String of any length).
 * <p>
 * For performance reasons, subwell data is always returned in arrays. There is one array per well
 * per feature, and the length of the array is equal to the number of subwell items in that well.
 * The array is sorted, so that the id of the subwell item is equal to its index in the array.
 * </p>
 * <p>
 * All data that is retrieved once, is cached for future lookups.
 * See {@link CacheService} for more information about caching.
 * </p>
 */
public class SubWellService  {

	private static SubWellService instance = new SubWellService();

	private ISubWellDataSource dataSource;
	private SubWellDataCache cache;

	private SubWellService() {
		// Hidden constructor.
		dataSource = DataSourceFactory.loadDataSource(Screening.getEnvironment());
		cache = new SubWellDataCache();
		EclipseLog.info("Using " + dataSource.getClass().getName(), Activator.getContext().getBundle());
	}

	public static SubWellService getInstance() {
		return instance;
	}

	// Note: used by DC to determine which file store type to use
	public boolean isHDF5DataSource() {
		return dataSource instanceof HDF5Datasource;
	}

	/**
	 * Get a sample subwell feature that has at least one point of subwell data.
	 * This can be used to determine the count of subwell items in a well, as all
	 * features should have the same amount of data for any given well.
	 * 
	 * @param well The well to retrieve a sample feature for.
	 * @return A sample feature, or null if no feature has data for this well.
	 */
	public SubWellFeature getSampleFeature(Well well) {
		if (well == null) return null;
		
		for (SubWellFeature f: ProtocolUtils.getProtocolClass(well).getSubWellFeatures()) {
			if (f.isNumeric()) {
				float[] data = getNumericData(well, f);
				if (data != null && data.length > 0) return f;
			} else {
				String[] data = getStringData(well, f);
				if (data != null && data.length > 0) return f;
			}
		}
		return null;
	}

	/**
	 * Get the data of a feature for a well, be it text or numeric.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The subwell feature to retrieve data for.
	 * @return The data, either a String array, a float array, or null.
	 */
	public Object getData(Well well, SubWellFeature feature) {
		if (well == null || feature == null) return null;

		if (feature.isNumeric()) return getNumericData(well, feature);
		else return getStringData(well, feature);
	}

	/**
	 * Get the numeric data of a feature for a well.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The name of the subwell feature to retrieve data for.
	 * @return The numeric data, or null if the well has no data for this feature.
	 */
	public Object getNumericData(Well well, String feature) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		SubWellFeature f = ProtocolUtils.getSubWellFeatureByName(feature, pClass);
		return getNumericData(well, f);
	}

	/**
	 * Get the numeric data of a feature for a well.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The subwell feature to retrieve data for.
	 * @return The numeric data, or null if the well has no data for this feature.
	 */
	public float[] getNumericData(Well well, SubWellFeature feature) {
		if (cache.isCached(well, feature)) {
			if (cache.isNumeric(well, feature)) return cache.getNumericData(well, feature);
			else return null;
		}
		float[] data = dataSource.getNumericData(well, feature);
		cache.putData(well, feature, data);
		return data;
	}

	/**
	 * Load a set of 2-dimensional (single-cell, timepoint-based) subwell data.
	 * 
	 * @deprecated Phaedra does not fully support 2D subwell data.
	 * @param well The well to retrieve data for.
	 * @param feature The subwell feature to retrieve data for.
	 * @return The 2D numeric data, or null if the well has no 2D data for this feature.
	 */
	/**
	 * Get the String data of a feature for a well.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The name of the subwell feature to retrieve data for.
	 * @return The String data, or null if the well has no data for this feature.
	 */
	public Object getStringData(Well well, String feature) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		SubWellFeature f = ProtocolUtils.getSubWellFeatureByName(feature, pClass);
		return getStringData(well, f);
	}

	/**
	 * Get the String data of a feature for a well.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The subwell feature to retrieve data for.
	 * @return The String data, or null if the well has no data for this feature.
	 */
	public String[] getStringData(Well well, SubWellFeature feature) {
		if (cache.isCached(well, feature)) {
			if (!cache.isNumeric(well, feature)) return cache.getStringData(well, feature);
			else return null;
		}
		String[] data = dataSource.getStringData(well, feature);
		cache.putData(well, feature, data);
		return data;
	}

	/**
	 * Get the number of subwell items for a given well.
	 * 
	 * @param well The well whose number of subwell items should be retrieved.
	 * @return The number of subwell items in the well, possibly 0.
	 */
	public int getNumberOfCells(Well well) {
		ProtocolClass pClass = ProtocolUtils.getProtocolClass(well);
		for (SubWellFeature feature : pClass.getSubWellFeatures()) {
			if (cache.isCached(well, feature)) {
				int dataSize = CollectionUtils.length(getData(well, feature));
				// This particular feature may not have any values. Use it only if it is not-empty.
				if (dataSize > 0) return dataSize;
			}
		}
		return dataSource.getNrCells(well);
	}

	/**
	 * Preload the data for a set of wells and a set of features.
	 * The data will be cached so that future lookups will be very fast.
	 * This may offer better performance than loading the data one well or one feature at a time.
	 * 
	 * @param wells The wells to preload data for.
	 * @param features The features to preload data for.
	 * @param monitor A progress monitor that will be updated during the load (optional)
	 */
	public void preloadData(List<Well> wells, List<SubWellFeature> features, IProgressMonitor monitor) {
		long start = System.currentTimeMillis();
		
		Set<Well> wellsToLoad = new HashSet<>();
		for (Well well: wells) {
			for (SubWellFeature feature: features) {
				if (!cache.isCached(well, feature)) wellsToLoad.add(well);
			}
		}
		if (!wellsToLoad.isEmpty()) dataSource.preloadData(new ArrayList<>(wellsToLoad), features, cache, monitor);

		long duration = System.currentTimeMillis() - start;
		EclipseLog.info(String.format("Data preload (%d wells, %d features): %d ms", wellsToLoad.size(), features.size(), duration), Activator.getContext().getBundle());
	}
	
	/**
	 * Update the subwell data for a set of wells in a single transaction.
	 * 
	 * @param dataMap The map of data to update, containing an array of data (String or float) per well.
	 * @param feature The subwell feature that the data belongs to.
	 * @throws IOException If the update transaction fails for any reason.
	 */
	public void updateData(Map<Well, Object> dataMap, SubWellFeature feature) throws IOException {
		if (feature == null || dataMap.isEmpty()) return;
		Map<SubWellFeature, Map<Well, Object>> data = new HashMap<>();
		data.put(feature, dataMap);
		updateData(data);
	}

	/**
	 * Update the subwell data for a set of wells and features in a single transaction.
	 * 
	 * @param data The map of data to update, containing a sub-map per feature. The sub-map contains
	 * an array of data (String or float) per well.
	 * @throws IOException If the update transaction fails for any reason.
	 */
	public void updateData(Map<SubWellFeature, Map<Well, Object>> data) throws IOException {
		dataSource.updateData(data);
	}

	/**
	 * Remove all subwell data for a given plate and feature from the cache.
	 * 
	 * @param plate The plate whose data will be cleared from the cache.
	 * @param feature The feature whose data will be cleared from the cache.
	 */
	public void removeFromCache(Plate plate, SubWellFeature feature) {
		for (Well well: plate.getWells()) {
			removeFromCache(well, feature);
		}
	}

	/**
	 * Remove all subwell data for a given well and feature from the cache.
	 * 
	 * @param well The well whose data will be cleared from the cache.
	 * @param feature The feature whose data will be cleared from the cache.
	 */
	public void removeFromCache(Well well, SubWellFeature feature) {
		cache.removeData(well, feature);
	}
}