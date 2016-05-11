package eu.openanalytics.phaedra.base.cache;

import java.io.Serializable;
import java.util.Arrays;

public class CacheKey implements Serializable {

	private static final long serialVersionUID = -1040985176667819827L;

	@IgnoreSizeOf
	private Object[] keyParts;

	public CacheKey(Object... keyParts) {
		this.keyParts = keyParts;
	}

	public Object getKeyPart(int i) {
		return keyParts[i];
	}
	
	public int getKeyLength() {
		return keyParts.length;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(keyParts);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CacheKey other = (CacheKey) obj;
		if (!Arrays.equals(keyParts, other.keyParts))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CacheKey [keyParts=" + Arrays.toString(keyParts) + "]";
	}

	public static CacheKey create(Object... keyParts) {
		return new CacheKey(keyParts);
	}
}
