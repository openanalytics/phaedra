package eu.openanalytics.phaedra.datacapture.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.fs.store.IFileStore;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

public class HDF5FileStore implements IFileStore {

	private String tempFolder;
	private HDF5File hdf5File;
	
	public HDF5FileStore() throws IOException {
		tempFolder = FileUtils.generateTempFolder(true);
		hdf5File = new HDF5File(tempFolder + "/filestore.h5", false);
	}
	
	public HDF5File getFile() {
		// Flush, close, reopen, so that it can be copied somewhere.
		hdf5File.close();
		hdf5File = new HDF5File(tempFolder + "/filestore.h5", false);
		return hdf5File;
	}
	
	@Override
	public void commit() throws IOException {
		// Do nothing.
	}
	
	@Override
	public void close() throws Exception {
		hdf5File.close();
		FileUtils.deleteRecursive(tempFolder);
	}
	
	@Override
	public void switchMode() throws IOException {
		// Do nothing.
	}
	
	@Override
	public String[] listKeys() throws IOException {
		List<String> keys = new ArrayList<>();
		
		String[] attributes = hdf5File.getAttributes("/");
		for (String attr: attributes) keys.add(getKeyForAttribute(attr));
		
		List<String> wellFeatures = hdf5File.getWellFeatures();
		for (String wf: wellFeatures) keys.add(DefaultDataCaptureStore.WELL_DATA_PREFIX + wf);
		
		// Note: will not return actual well numbers, just the feature names for well 1.
		List<String> swFeatures = hdf5File.getSubWellFeatures();
		for (String swf: swFeatures) keys.add(String.format("%s1.%s", DefaultDataCaptureStore.WELL_SWDATA_PREFIX, swf));
		
		return keys.toArray(new String[keys.size()]);
	}
	
	@Override
	public String readStringValue(String key) throws IOException {
		Object value = hdf5File.getAttribute(getPath(key), getAttribute(key));
		return value == null ? null : String.valueOf(value);
	}
	
	@Override
	public float readNumericValue(String key) throws IOException {
		Object value = hdf5File.getAttribute(getPath(key), getAttribute(key));
		return value == null ? Float.NaN : (Float) value;
	}
	
	@Override
	public String[] readStringArray(String key) throws IOException {
		return hdf5File.getStringData1D(getPath(key));
	}
	
	@Override
	public float[] readNumericArray(String key) throws IOException {
		return hdf5File.getNumericData1D(getPath(key));
	}
	
	@Override
	public byte[] readBinaryValue(String key) throws IOException {
		return StreamUtils.readAll(hdf5File.getBinaryData(getPath(key)));
	}
	
	@Override
	public Object readValue(String key) throws IOException {
		return hdf5File.getAnyData1D(getPath(key), 0);
	}
	
	@Override
	public void writeStringValue(String key, String value) throws IOException {
		hdf5File.setAttribute(getPath(key), getAttribute(key), value);
	}
	
	@Override
	public void writeNumericValue(String key, float value) throws IOException {
		hdf5File.setAttribute(getPath(key), getAttribute(key), value);
	}
	
	@Override
	public void writeStringArray(String key, String[] value) throws IOException {
		hdf5File.writeStringData1D(getPath(key), value);
	}

	@Override
	public void writeNumericArray(String key, float[] value) throws IOException {
		hdf5File.writeNumericData(getPath(key), value);
	}

	@Override
	public void writeBinaryValue(String key, byte[] value) throws IOException {
		hdf5File.writeBinaryData(getPath(key), value);
	}

	@Override
	public void writeValue(String key, Object value) throws IOException {
		if (value instanceof String) writeStringValue(key, (String) value);
		else if (value instanceof Float) writeNumericValue(key, (Float) value);
		else if (value instanceof String[]) writeStringArray(key, (String[]) value);
		else if (value instanceof float[]) writeNumericArray(key, (float[]) value);
		else if (value instanceof byte[]) writeBinaryValue(key, (byte[]) value);
	}

	private String getPath(String key) {
		if (key.startsWith(DefaultDataCaptureStore.WELL_DATA_PREFIX)) {
			String featureId = key.substring(DefaultDataCaptureStore.WELL_DATA_PREFIX.length());
			return HDF5File.getWellDataPath(featureId);
		} else if (key.startsWith(DefaultDataCaptureStore.WELL_SWDATA_PREFIX)) {
			key = key.substring(DefaultDataCaptureStore.WELL_SWDATA_PREFIX.length());
			int wellNr = Integer.parseInt(key.split("\\.")[0]);
			String featureId = key.substring(key.indexOf('.') + 1);
			return HDF5File.getSubWellDataPath(wellNr, featureId);
		}
		return "/";
	}
	
	private String getAttribute(String key) {
		if (key.startsWith(DefaultDataCaptureStore.PLATE_PROPERTY_PREFIX)) {
			return key.substring(DefaultDataCaptureStore.PLATE_PROPERTY_PREFIX.length());
		} else if (key.startsWith(DefaultDataCaptureStore.WELL_PROPERTY_PREFIX)) {
			return "well" + key.substring(DefaultDataCaptureStore.WELL_PROPERTY_PREFIX.length());
		}
		return key;
	}
	
	private String getKeyForAttribute(String attributeName) {
		if (attributeName.startsWith("well")) return DefaultDataCaptureStore.WELL_PROPERTY_PREFIX + attributeName.substring(4);
		else if (attributeName.equals(DefaultDataCaptureStore.KEY_IMAGE_PATH)) return DefaultDataCaptureStore.KEY_IMAGE_PATH;
		else return DefaultDataCaptureStore.PLATE_PROPERTY_PREFIX + attributeName;
	}
}