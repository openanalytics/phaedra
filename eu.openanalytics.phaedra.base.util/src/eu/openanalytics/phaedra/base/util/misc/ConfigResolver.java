package eu.openanalytics.phaedra.base.util.misc;

import java.io.IOException;
import java.util.function.Function;

public abstract class ConfigResolver {
	
	private String sysPropPrefix;
	
	private Function<String, String> resolver;
	private EncryptedResolver encryptedResolver;
	private Function<String, String[]> keySupplier;
	
	public ConfigResolver(String sysPropPrefix) {
		this.sysPropPrefix = sysPropPrefix;
	}
	
	public String get(String key) {
		String value = null;
		if (sysPropPrefix != null) value = System.getProperty(sysPropPrefix + key);
		if (resolver != null && (value == null || value.isEmpty())) value = resolver.apply(key);
		return value;
	}
	
	public String get(String key, String defaultValue) {
		String value = null;
		if (sysPropPrefix != null) value = System.getProperty(sysPropPrefix + key);
		if (resolver != null && (value == null || value.isEmpty())) value = resolver.apply(key);
		if (value == null || value.isEmpty()) return value = defaultValue;
		return value;
	}
	
	public String getEncrypted(String key) {
		String value = null;
		if (sysPropPrefix != null) value = System.getProperty(sysPropPrefix + key);
		if (encryptedResolver != null && (value == null || value.isEmpty())) {
			try {
				value = encryptedResolver.resolve(key);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return value;
	}
	
	public String[] getKeys(String prefix) {
		if (keySupplier != null) return keySupplier.apply(prefix);
		return new String[0];
	}

	public void setResolver(Function<String, String> resolver) {
		this.resolver = resolver;
	}
	
	public void setEncryptedResolver(EncryptedResolver encryptedResolver) {
		this.encryptedResolver = encryptedResolver;
	}
	
	public void setKeySupplier(Function<String, String[]> keySupplier) {
		this.keySupplier = keySupplier;
	}
	
	public static interface EncryptedResolver {
		public String resolve(String key) throws IOException;
	}
}
