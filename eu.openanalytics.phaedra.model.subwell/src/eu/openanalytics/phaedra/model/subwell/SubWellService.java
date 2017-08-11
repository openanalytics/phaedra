package eu.openanalytics.phaedra.model.subwell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.cache.SubWellDataCache;
import eu.openanalytics.phaedra.model.subwell.data.DBDataSource;
import eu.openanalytics.phaedra.model.subwell.data.ISubWellDataSource;

public class SubWellService  {

	private static SubWellService instance = new SubWellService();

	private ISubWellDataSource dataSource;
	private SubWellDataCache cache;

	private SubWellService() {
		// Hidden constructor.
//		dataSource = new HDF5Datasource();
		dataSource = new DBDataSource();
		cache = new SubWellDataCache();
	}

	public static SubWellService getInstance() {
		return instance;
	}

	/*
	 * ********
	 * Read API
	 * ********
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

	public Object getData(Well well, SubWellFeature feature) {
		if (well == null || feature == null) return null;

		if (feature.isNumeric()) return getNumericData(well, feature);
		else return getStringData(well, feature);
	}

	public Object getNumericData(Well well, String feature) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		SubWellFeature f = ProtocolUtils.getSubWellFeatureByName(feature, pClass);
		return getNumericData(well, f);
	}

	public float[] getNumericData(Well well, SubWellFeature feature) {
		if (cache.isCached(well, feature)) {
			if (cache.isNumeric(well, feature)) return cache.getNumericData(well, feature);
			else return null;
		}
		float[] data = dataSource.getNumericData(well, feature);
		cache.putData(well, feature, data);
		return data;
	}

	public Object getStringData(Well well, String feature) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		SubWellFeature f = ProtocolUtils.getSubWellFeatureByName(feature, pClass);
		return getStringData(well, f);
	}

	public String[] getStringData(Well well, SubWellFeature feature) {
		if (cache.isCached(well, feature)) {
			if (!cache.isNumeric(well, feature)) return cache.getStringData(well, feature);
			else return null;
		}
		String[] data = dataSource.getStringData(well, feature);
		cache.putData(well, feature, data);
		return data;
	}

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

	public void preloadData(Well well, IProgressMonitor monitor) {
		preloadData(Collections.singletonList(well), ProtocolUtils.getSubWellFeatures(well), monitor);
	}
	
	public void preloadData(Plate plate, IProgressMonitor monitor) {
		preloadData(PlateService.streamableList(plate.getWells()), ProtocolUtils.getSubWellFeatures(plate), monitor);
	}
	
	public void preloadData(List<Well> wells, List<SubWellFeature> features, IProgressMonitor monitor) {
		Set<Well> wellsToLoad = new HashSet<>();
		for (Well well: wells) {
			for (SubWellFeature feature: features) {
				if (!cache.isCached(well, feature)) wellsToLoad.add(well);
			}
		}
		if (!wellsToLoad.isEmpty()) dataSource.preloadData(new ArrayList<>(wellsToLoad), features, cache, monitor);
	}

	public void updateData(Map<SubWellFeature, Map<Well, Object>> data) {
		dataSource.updateData(data);
	}

	public void removeFromCache(Plate plate, SubWellFeature feature) {
		for (Well well: plate.getWells()) {
			removeFromCache(well, feature);
		}
	}

	public void removeFromCache(Well well, SubWellFeature feature) {
		cache.removeData(well, feature);
	}
}