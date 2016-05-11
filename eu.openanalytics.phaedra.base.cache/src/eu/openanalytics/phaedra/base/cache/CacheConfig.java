package eu.openanalytics.phaedra.base.cache;

import eu.openanalytics.phaedra.base.cache.prefs.Prefs;


public class CacheConfig {

	public String name;
	public boolean useDisk;
	public int ttl;
	public int tti;
	
	public CacheConfig() {
		this(null);
	}
	
	public CacheConfig(String name) {
		this.name = name;
		this.useDisk = Prefs.getBoolean(Prefs.DEFAULT_CACHE_USE_DISK);
		this.ttl = Prefs.getInt(Prefs.DEFAULT_CACHE_TTL);
		this.tti = Prefs.getInt(Prefs.DEFAULT_CACHE_TTI);
	}
	
	public CacheConfig(String name, boolean useDisk) {
		this(name);
		this.useDisk = useDisk;
	}
}
