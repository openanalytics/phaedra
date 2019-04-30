package eu.openanalytics.phaedra.base.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import eu.openanalytics.phaedra.base.cache.impl.EhCacheDisk;
import eu.openanalytics.phaedra.base.cache.impl.EhCacheHeap;
import eu.openanalytics.phaedra.base.cache.prefs.Prefs;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

/**
 * <p>Manages all the caches in Phaedra.</p>
 * 
 * <p>All caches added together cannot exceed M% of the total heap size.
 * The value of M can be configured via the preference CACHE_HEAP_PCT.</p>
 *
 * <p>There is always at least one cache available: the default cache.
 * This is a general-purpose cache with default settings.
 * </p>
 *
 * <p>TimeToLive: Time to live since added.</p>
 * <p>TimeToIdle: Time to live since last used.</p>
 */
public class CacheService {

	private static final String DEFAULT_CACHE_NAME = "defaultCache";
	
	public static final double CACHE_HEAP_PCT_MIN = 0.10;
	public static final double CACHE_HEAP_PCT_MAX = 0.90;
	
	private static CacheService instance;
	
	private CacheManager cacheManager;
	private Map<String, ICache> cacheMap;
	
	private long maxHeapSizeBytes;
	private long maxDiskSizeBytes;
	
	private CacheService() {
		cacheMap = new ConcurrentHashMap<>();
		
		double heapPct = Prefs.getInt(Prefs.CACHE_HEAP_PCT) / 100.0;
		heapPct = Math.max(Math.min(heapPct, CACHE_HEAP_PCT_MAX), CACHE_HEAP_PCT_MIN);
		
		maxHeapSizeBytes = (long) (Runtime.getRuntime().maxMemory() * heapPct);
		maxDiskSizeBytes = ((long) Prefs.getInt(Prefs.CACHE_DISK_SIZE)) * 1024 * 1024;
		
		// Create the CacheManager that will create (byte)size based caches.
		Configuration config = new Configuration()
			.name("maxBytesManager")
			.maxBytesLocalHeap(maxHeapSizeBytes, MemoryUnit.BYTES)
			.sizeOfPolicy(new SizeOfPolicyConfiguration().maxDepth(5000))
		;
		if (maxDiskSizeBytes > 0) config.maxBytesLocalDisk(maxDiskSizeBytes, MemoryUnit.BYTES);

		cacheManager = CacheManager.newInstance(config);

		// There is always a DEFAULT cache.
		cacheMap.put(DEFAULT_CACHE_NAME, doCreateCache(new CacheConfig(DEFAULT_CACHE_NAME)));
	}

	public static synchronized CacheService getInstance() {
		if (instance == null) instance = new CacheService();
		return instance;
	}

	public long getMaxHeapSizeBytes() {
		return maxHeapSizeBytes;
	}
	
	public long getMaxDiskSizeBytes() {
		return maxDiskSizeBytes;
	}
	
	/**
	 * <p>Get the names of all known and active caches.</p>
	 * 
	 * @return The names of all caches.
	 */
	public String[] getAllCaches() {
		return cacheMap.keySet().toArray(new String[cacheMap.size()]);
	}
	
	/**
	 * <p>Get the default cache, which is a general-purpose cache with default settings.</p>
	 * 
	 * @return The default cache.
	 */
	public ICache getDefaultCache() {
		return getCache(DEFAULT_CACHE_NAME);
	}
	
	/**
	 * <p>Get the cache associated with the given name.</p>
	 * 
	 * @param name The name of the cache to retrieve.
	 * @return The cache with the specified name.
	 * @throws IllegalArgumentException If no cache with the given name exists.
	 */
	public ICache getCache(String name) {
		if (name == null) throw new IllegalArgumentException("Illegal cache name '" + name + "'");
		ICache cache = cacheMap.get(name);
		if (cache == null) throw new IllegalArgumentException("No cache with name '" + name + "'");
		return cache;
	}

	/**
	 * <p>Create a new cache on the fly, with the given name and default settings.</p>
	 * <p>The cache must have a unique name. It will be managed together with existing caches,
	 * and can be destroyed using {@link deleteCache}.</p>
	 * 
	 * @param name The name for the new cache.
	 * @return A new, ready-to-use cache.
	 */
	public ICache createCache(String name) {
		CacheConfig config = new CacheConfig(name);
		return createCache(config);
	}
	
	/**
	 * <p>Create a new cache on the fly.</p>
	 * <p>The cache must have a unique name. It will be managed together with existing caches,
	 * and can be destroyed using {@link deleteCache}.</p>
	 * 
	 * @param config The configuration for the new cache.
	 * @return A new, ready-to-use cache.
	 */
	public ICache createCache(CacheConfig config) {
		if (config == null || config.name == null || config.name.isEmpty()) throw new IllegalArgumentException("Invalid cache configuration");
		if (cacheMap.containsKey(config.name)) throw new IllegalArgumentException("A cache with name '" + config.name + "' already exists");
		ICache cache = doCreateCache(config);
		cacheMap.put(config.name, cache);
		return cache;
	}
	
	/**
	 * <p>Delete a cache. The cache will be emptied and no longer exists.
	 * Note that this method does NOT perform custom actions on the cached items,
	 * such as {@link org.eclipse.swt.graphics.Resource#dispose}</p>
	 * 
	 * @param cache The cache to delete.
	 */
	public void deleteCache(ICache cache) {
		if (cache == null) return;
		String name = cacheMap.keySet().stream().filter(s -> cacheMap.get(s) == cache).findFirst().orElse(null);
		if (name == null) return;
		cacheMap.remove(name);
		cacheManager.removeCache(name);
	}
	
	/**
	 * <p>Shutdown the CacheManager and all caches managed by it.</p>
	 *
	 * <p>Should only be called once on Phaedra exit.</p>
	 */
	public void shutdown() {
		try {
			if (cacheManager != null && cacheManager.getStatus() == Status.STATUS_ALIVE) cacheManager.shutdown();
		} catch (Exception e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
	}

	/**
	 * <p>Create a new cache managed by this CacheService.</p>
	 *
	 * @param config The CacheConfiguration object containing the settings for the new cache.
	 * @return A new cache instance, fully configured and ready for use.
	 */
	@SuppressWarnings("deprecation")
	private ICache doCreateCache(CacheConfig config) {
		CacheConfiguration cacheConfiguration = new CacheConfiguration(config.name, 0)
			.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
			.timeToLiveSeconds(config.ttl)
			.timeToIdleSeconds(config.tti)
		;

		if (config.maxBytes > 0) {
			cacheConfiguration.maxBytesLocalHeap(config.maxBytes, MemoryUnit.BYTES);
		}
		
		boolean diskStoreEnabled = config.useDisk && maxDiskSizeBytes > 0;
		if (diskStoreEnabled) {
			cacheConfiguration
				.diskSpoolBufferSizeMB(Prefs.getInt(Prefs.CACHE_DISK_BUFFER_SIZE))
				.persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP));
		} else {
			// Deprecated??? It's the only way to disable disk overflow for an individual cache... persistence strategy NONE doesn't work.
			cacheConfiguration.overflowToDisk(false);
		}
		
		Cache cache = new Cache(cacheConfiguration);
		cacheManager.addCache(cache);
		
		if (diskStoreEnabled) return new EhCacheDisk(config.name, cache);
		else return new EhCacheHeap(config.name, cache);
	}
}
