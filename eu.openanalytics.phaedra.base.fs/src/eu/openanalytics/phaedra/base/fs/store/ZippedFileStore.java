package eu.openanalytics.phaedra.base.fs.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AccessMode;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import net.java.truevfs.comp.zip.ZipEntry;
import net.java.truevfs.comp.zip.ZipFile;
import net.java.truevfs.comp.zip.ZipOutputStream;

public class ZippedFileStore implements IFileStore {

	private SecureFileServer fs;
	private String fsPath;
	private AccessMode mode;
	
	private ZipFile input;
	private Map<String, ZipEntry> inputEntries;
	
	private ZipOutputStream output;
	private String tempOutputPath;

	public ZippedFileStore(String fsPath, AccessMode mode, SecureFileServer fs) throws IOException {
		this.fsPath = fsPath;
		this.mode = mode;
		this.fs = fs;

		if (mode == AccessMode.READ) {
			input = new ZipFile(fs.getChannel(fsPath, "r"));
			readZipEntries();
		} else {
			tempOutputPath = FileUtils.generateTempFolder(true) + "/filestore.zip";
			output = new ZipOutputStream(new FileOutputStream(tempOutputPath));
		}
	}

	@Override
	public void close() throws Exception {
		if (inputEntries != null) inputEntries.clear();
		if (input != null) try { input.close(); } catch (IOException e) {}
		if (output != null) try { output.close(); } catch (IOException e) {}
		if (tempOutputPath != null) FileUtils.deleteRecursive(new File(tempOutputPath).getParentFile());
	}
	
	@Override
	public void commit() throws IOException {
		try { output.close(); } catch (IOException e) {}
		if (fs != null) fs.putContents(fsPath, new File(tempOutputPath));
	}
	
	@Override
	public void switchMode() throws IOException {
		if (mode == AccessMode.WRITE) {
			mode = AccessMode.READ;
			output.close();
			input = new ZipFile(Paths.get(tempOutputPath));
			readZipEntries();
		} else {
			//TODO Switch from read to write mode?
		}
	}
	
	@Override
	public String[] listKeys() throws IOException {
		//TODO properly implement switching
		if (mode == AccessMode.WRITE) switchMode();
		return inputEntries.keySet().stream().sorted().toArray(i -> new String[i]);
	}
	
	@Override
	public String readStringValue(String key) throws IOException {
		Object value = readValue(key);
		if (value == null) return null;
		return String.valueOf(value);
	}

	@Override
	public float readNumericValue(String key) throws IOException {
		return parseEntry(key, float.class);
	}

	@Override
	public String[] readStringArray(String key) throws IOException {
		return parseEntry(key, String[].class);
	}

	@Override
	public float[] readNumericArray(String key) throws IOException {
		return parseEntry(key, float[].class);
	}

	@Override
	public byte[] readBinaryValue(String key) throws IOException {
		return parseEntry(key, byte[].class);
	}

	@Override
	public Object readValue(String key) throws IOException {
		return parseEntry(key, Object.class);
	}
	
	@Override
	public void writeStringValue(String key, String value) throws IOException {
		writeEntry(key, value);
		
	}

	@Override
	public void writeNumericValue(String key, float value) throws IOException {
		writeEntry(key, value);
	}

	@Override
	public void writeStringArray(String key, String[] value) throws IOException {
		writeEntry(key, value);
	}

	@Override
	public void writeNumericArray(String key, float[] value) throws IOException {
		writeEntry(key, value);
	}

	@Override
	public void writeBinaryValue(String key, byte[] value) throws IOException {
		writeEntry(key, value);
	}
	
	@Override
	public void writeValue(String key, Object value) throws IOException {
		writeEntry(key, value);
	}
	
	@SuppressWarnings("unchecked")
	private <E> E parseEntry(String key, Class<E> dataType) throws IOException {
		if (mode != AccessMode.READ) throw new IOException("Cannot read entry: file is write-only");
		
		ZipEntry entry = inputEntries.get(key);
		if (entry == null) return null; //throw new IOException("Key not found: " + key);
		
		try (InputStream i = input.getInputStream(entry.getName())) {
			ObjectInputStream is = new ObjectInputStream(i);
			Object value = is.readObject();
			//TODO Make sure value is compatible with dataType
			return (E) value;
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}
	
	private synchronized void writeEntry(String key, Object data) throws IOException {
		if (mode != AccessMode.WRITE) throw new IOException("Cannot write entry: file is read-only");
		
		byte[] serialized = null;
		if (data instanceof  byte[]) {
			serialized = (byte[]) data;
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(bos);
			os.writeObject(data);
			serialized = bos.toByteArray();
		}
		
		ZipEntry zipEntry = new ZipEntry(key);
		zipEntry.setTime(System.currentTimeMillis());
		zipEntry.setSize(serialized.length);
		zipEntry.setCompressedSize(serialized.length);
		zipEntry.setCrc(StreamUtils.calculateCRC(new ByteArrayInputStream(serialized)));
		zipEntry.setMethod(ZipEntry.STORED);
		output.putNextEntry(zipEntry);
		StreamUtils.copy(new ByteArrayInputStream(serialized), output);
	}
	
	private void readZipEntries() {
		inputEntries = new HashMap<>();
		Enumeration<? extends ZipEntry> zipEntries = input.entries();
		while (zipEntries.hasMoreElements()) {
			ZipEntry entry = zipEntries.nextElement();
			if (entry.isDirectory()) continue;
			inputEntries.put(entry.getName(), entry);
		}
	}
}
