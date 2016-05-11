package eu.openanalytics.phaedra.calculation.norm.cache;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class NormalizationCache {

	private ICache cache;

	public NormalizationCache() {
		this.cache = CacheService.getInstance().createCache(NormalizationCache.class.getSimpleName());
	}

	public void putGrid(IValueObject object, IFeature f, String norm, NormalizedGrid grid) {
		cache.put(createKey(object, f, norm), grid);
	}

	public NormalizedGrid getGrid(IValueObject object, IFeature f, String norm) {
		return (NormalizedGrid) cache.get(createKey(object, f, norm));
	}

	public void purge(IValueObject object) {
		if (object == null) return;
		cache.getKeys().stream()
			.filter(k -> (object.getClass() == ((CacheKey)k).getKeyPart(0)) && object.getId() == (long)((CacheKey)k).getKeyPart(1))
			.forEach(k -> cache.remove(k));
	}

	private CacheKey createKey(IValueObject object, IFeature feature, String norm) {
		return CacheKey.create(object.getClass(), object.getId(), feature.getId(), norm);
	}

}
