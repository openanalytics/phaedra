package eu.openanalytics.phaedra.model.subwell.cache;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class SubWellDataCache {

	private ICache cache;

	public SubWellDataCache() {
		this.cache = CacheService.getInstance().createCache(SubWellDataCache.class.getSimpleName());
	}

	public boolean isCached(Well well, SubWellFeature feature) {
		return cache.contains(createKey(well, feature));
	}

	public boolean isNumeric(Well well, SubWellFeature feature) {
		Object key = createKey(well, feature);
		return cache.get(key) instanceof float[];
	}

	public float[] getNumericData(Well well, SubWellFeature feature) {
		Object key = createKey(well, feature);
		Object o = cache.get(key);
		if (o instanceof float[]) return (float[]) o;
		return null;
	}

	public String[] getStringData(Well well, SubWellFeature feature) {
		Object key = createKey(well, feature);
		Object o = cache.get(key);
		if (o instanceof String[]) return (String[]) o;
		return null;
	}

	public void putData(Well well, SubWellFeature feature, float[] data) {
		cache.put(createKey(well, feature), data);
	}

	public void putData(Well well, SubWellFeature feature, String[] data) {
		cache.put(createKey(well, feature), data);
	}

	public void removeData(Well well, SubWellFeature feature) {
		cache.remove(createKey(well, feature));
	}

	private Object createKey(Well well, SubWellFeature feature) {
		return CacheKey.create(well, feature);
	}

	//TODO Purge data
}