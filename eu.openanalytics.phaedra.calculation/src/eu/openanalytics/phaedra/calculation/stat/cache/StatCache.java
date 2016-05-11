package eu.openanalytics.phaedra.calculation.stat.cache;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.calculation.stat.StatQuery;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class StatCache {

	private ICache cache;

	public StatCache() {
		this.cache = CacheService.getInstance().createCache(StatCache.class.getSimpleName());
	}

	public StatContainer getStats(StatQuery query) {
		return (StatContainer) cache.get(createKey(query));
	}

	public void add(StatQuery query, StatContainer stats) {
		cache.put(createKey(query), stats);
	}

	public void remove(IValueObject object) {
		if (object == null) return;
		cache.getKeys().stream()
			.filter(k -> (object.getClass() == ((CacheKey)k).getKeyPart(0)) && object.getId() == (long)((CacheKey)k).getKeyPart(1))
			.forEach(k -> cache.remove(k));
	}

	private Object createKey(StatQuery query) {
		return createKey(query.getObject(), query.getFeature(), query.getNormalization(), query.getWellType(), query.isIncludeRejected());
	}

	private Object createKey(IValueObject object, IFeature feature, String norm, String wellType, boolean isIncludeRejected) {
		return CacheKey.create(object.getClass(), object.getId(), feature.getId(), norm, wellType, isIncludeRejected);
	}

}
