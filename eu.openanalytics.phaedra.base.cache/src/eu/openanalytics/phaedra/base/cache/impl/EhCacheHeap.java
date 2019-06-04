package eu.openanalytics.phaedra.base.cache.impl;

import java.util.List;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.ICache;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

public class EhCacheHeap implements ICache {

	private String name;
	private Cache cache;

	public EhCacheHeap(String name, Cache cache) {
		this.name = name;
		this.cache = cache;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public long getHeapSize() {
		return cache.getStatistics().getLocalHeapSizeInBytes();
	}
	
	@Override
	public long getDiskSize() {
		return cache.getStatistics().getLocalDiskSizeInBytes();
	}
	
	@Override
	public Object get(Object key) {
		Element element = cache.get(key);
		if (element == null) return null;
		return element.getObjectValue();
	}

	@Override
	public Object put(Object key, Object value) {
		cache.put(new Element(key, value));
		return value;
	}

	@Override
	public boolean remove(Object key) {
		return cache.remove(key);
	}
	
	@Override
	public boolean remove(CacheKey key, boolean allowWildcards) {
		getKeys().stream()
			.filter(k -> k instanceof CacheKey)
			.filter(k -> isWildcardMatch(key, (CacheKey)k))
			.forEach(k -> cache.remove(k));
		return false;
	}

	private boolean isWildcardMatch(CacheKey k1, CacheKey k2) {
		if (k1 == null && k2 == null) return true;
		if (k1 == null || k2 == null) return false;
		if (k1.getKeyLength() != k2.getKeyLength()) return false;
		for (int i=0; i<k1.getKeyLength(); i++) {
			Object o1 = k1.getKeyPart(i);
			Object o2 = k2.getKeyPart(i);
			if (o1 != null && !o1.equals(o2)) return false;
		}
		return true;
	}
	
	@Override
	public boolean contains(Object key) {
		// Workaround: isKeyInCache may return true for expired elements.
		// Fetching the element here is slower, but forces eviction of expired elements.
		cache.get(key);
		return cache.isKeyInCache(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object> getKeys() {
		return cache.getKeys();
	}

	@Override
	public void clear() {
		cache.removeAll();
	}
}
