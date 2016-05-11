package eu.openanalytics.phaedra.base.cache;

import java.util.List;

public interface ICache {

	public String getName();
	
	public long getHeapSize();
	
	public long getDiskSize();
	
	public Object get(Object key);

	public Object put(Object key, Object value);

	public boolean remove(Object key);

	public boolean remove(CacheKey key, boolean allowWildcards);

	public boolean contains(Object key);

	public List<Object> getKeys();

	public void clear();
}
