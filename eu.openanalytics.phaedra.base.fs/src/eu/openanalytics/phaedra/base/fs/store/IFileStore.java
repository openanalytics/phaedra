package eu.openanalytics.phaedra.base.fs.store;

import java.io.IOException;

public interface IFileStore extends AutoCloseable {
	
	/**
	 * Commit the data into the store.
	 * Can only be used if the store is in WRITE mode.
	 * <p>
	 * Note that if you close a WRITE-mode store without committing it first,
	 * any data written will be lost.
	 * </p>
	 * @throws IOException If the write fails for any reason.
	 */
	public void commit() throws IOException;
	
	public String[] listKeys() throws IOException;
	
	public String readStringValue(String key) throws IOException;
	public float readNumericValue(String key) throws IOException;
	public String[] readStringArray(String key) throws IOException;
	public float[] readNumericArray(String key) throws IOException;
	public byte[] readBinaryValue(String key) throws IOException;
	public Object readValue(String key) throws IOException;
	
	public void writeStringValue(String key, String value) throws IOException;
	public void writeNumericValue(String key, float value) throws IOException;
	public void writeStringArray(String key, String[] value) throws IOException;
	public void writeNumericArray(String key, float[] value) throws IOException;
	public void writeBinaryValue(String key, byte[] value) throws IOException;
	public void writeValue(String key, Object value) throws IOException;
}
