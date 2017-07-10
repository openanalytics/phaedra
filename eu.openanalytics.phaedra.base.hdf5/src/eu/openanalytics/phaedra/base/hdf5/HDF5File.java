package eu.openanalytics.phaedra.base.hdf5;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

/**
 * High-level interface to HDF5 files. Read and write methods on specific locations:
 *
 * <ul>
 * <li>Well data</li>
 * <li>Subwell data</li>
 * <li>Image data</li>
 * <li>Extra data</li>
 * <li>Meta data</li>
 * <li>Gates</li>
 * </ul>
 */
public class HDF5File extends BaseHDF5File implements AutoCloseable {

	public HDF5File(String path, boolean readonly) {
		super(path, readonly);
	}

	public static HDF5File openForRead(String fsPath) {
		SecureFileServer fs = Screening.getEnvironment().getFileServer();
		return new HDF5File(fs.getAsFile(fsPath).getAbsolutePath(), true);
	}
	
	/*
	 * *********
	 * Well data
	 * *********
	 */

	public List<String> getWellFeatures() {
		List<String> unescapedFeatures = new ArrayList<String>();
		try {
			if (exists(getWellDataPath())) {
				for (String feature : getChildren(getWellDataPath())) {
					unescapedFeatures.add(unescape(feature));
				}
			}
		} catch (IOException e) {
			// No features available.
		}
		return unescapedFeatures;
	}

	public boolean isWellDataNumeric(String featureId) {
		String path = getWellDataPath(featureId);
		return isNumeric(getDatasetClass(path));
	}

	public boolean existsWellData(String featureId) {
		String path = getWellDataPath(featureId);
		return exists(path);
	}

	public boolean existsWellData() {
		return exists(getWellDataPath());
	}

	public void writeWellData(String[] data, String featureId) throws IOException {
		String path = getWellDataPath(featureId);
		writeStringData1D(path, data);
	}

	public void writeWellData(float[] data, String featureId) throws IOException {
		String path = getWellDataPath(featureId);
		writeNumericData(path, data);
	}

	public float[] getNumericWellData(String featureId) throws IOException {
		if (!existsWellData(featureId)) {
			throw new IOException("No well data for " + featureId);
		}
		if (!isWellDataNumeric(featureId)) {
			throw new IOException("Well data is not numeric for " + featureId);
		}
		String path = getWellDataPath(featureId);
		float[] numericValues = getNumericData1D(path);
		return numericValues;
	}

	public String[] getStringWellData(String featureId) throws IOException {
		if (!existsWellData(featureId)) {
			throw new IOException("No well data for " + featureId);
		}
		if (isWellDataNumeric(featureId)) {
			throw new IOException("Well data is numeric for " + featureId);
		}
		String path = getWellDataPath(featureId);
		String[] stringValues = getStringData1D(path);
		return stringValues;
	}

	/*
	 * *************
	 * Sub-well data
	 * *************
	 */

	public List<String> getSubWellFeatures() {
		List<String> unescapedFeatures = new ArrayList<String>();
		try {
			if (exists(getSubWellDataPath())) {
				for (String feature : getChildren(getSubWellDataPath())) {
					unescapedFeatures.add(unescape(feature));
				}
			}
		} catch (IOException e) {
			// No features available.
		}
		return unescapedFeatures;
	}

	public boolean isSubWellDataNumeric(String featureId, int wellNr) {
		String path = getSubWellDataPath(wellNr, featureId);
		return isNumeric(getDatasetClass(path));
	}

	public boolean existsSubWellData() {
		return exists(getSubWellDataPath());
	}

	public boolean existsSubWellData(String featureId, int wellNr) {
		String path = getSubWellDataPath(wellNr, featureId);
		return exists(path);
	}

	public void writeSubWellData(String[] data, int wellNr, String featureId) throws IOException {
		String path = getSubWellDataPath(wellNr, featureId);
		writeStringData1D(path, data);
	}

	public void writeSubWellData(float[] data, int wellNr, String featureId) throws IOException {
		String path = getSubWellDataPath(wellNr, featureId);
		writeNumericData(path, data);
	}

	public float[] getNumericSubWellData(String featureId, int wellNr) throws IOException {
		return getNumericSubWellData(featureId, wellNr, 1);
	}

	public float[] getNumericSubWellData(String featureId, int wellNr, int stride) throws IOException {
		if (!existsSubWellData(featureId, wellNr)) {
			throw new IOException("No sub-well data for " + featureId);
		}
		if (!isSubWellDataNumeric(featureId, wellNr)) {
			throw new IOException("Sub-well data is not numeric for " + featureId);
		}
		String path = getSubWellDataPath(wellNr, featureId);
		return getNumericData1D(path, stride);
	}

	public String[] getStringSubWellData(String featureId, int wellNr) throws IOException {
		return getStringSubWellData(featureId, wellNr, 1);
	}

	public String[] getStringSubWellData(String featureId, int wellNr, int stride) throws IOException {
		if (!existsSubWellData(featureId, wellNr)) {
			throw new IOException("No sub-well data for " + featureId);
		}
		if (isSubWellDataNumeric(featureId, wellNr)) {
			throw new IOException("Sub-well data is numeric for " + featureId);
		}
		String path = getSubWellDataPath(wellNr, featureId);
		return getStringData1D(path, stride);
	}

	/*
	 * **********
	 * Image data
	 * **********
	 */

	public boolean existsImageData(String imageId) {
		return exists(getImageDataPath() + "/" + imageId);
	}

	public void writeImageData(byte[] data, String imageId) throws IOException {
		String path = getImageDataPath() + "/" + imageId;
		writeBinaryData(path, data);
	}

	public void writeImageData(InputStream data, String imageId) throws IOException {
		byte[] bytes = StreamUtils.readAll(data);
		writeImageData(bytes, imageId);
	}

	public void writeImageData(InputStream data, String imageId, long size) throws IOException {
		String path = getImageDataPath() + "/" + imageId;
		writeBinaryData(path, data, size);
	}

	public InputStream getImageData(String imageId) throws IOException {
		String path = getImageDataPath() + "/" + imageId;
		return getBinaryData(path);
	}

	/*
	 * **********
	 * Extra data
	 * **********
	 */

	public void writeExtraData(byte[] data, String id) throws IOException {
		String path = getExtraDataPath() + "/" + id;
		writeBinaryData(path, data);
	}

	public void writeExtraData(InputStream data, String id) throws IOException {
		byte[] bytes = StreamUtils.readAll(data);
		writeExtraData(bytes, id);
	}

	public void writeExtraData(InputStream data, String id, long size) throws IOException {
		String path = getExtraDataPath() + "/" + id;
		writeBinaryData(path, data, size);
	}

	public InputStream getExtraData(String id) throws IOException {
		String path = getExtraDataPath() + "/" + id;
		return getBinaryData(path);
	}

	/*
	 * *********
	 * Meta data
	 * *********
	 */

	public void writeMetaData(Map<String, Map<String, String>> metadataMap, boolean keepPreviousMetaData) throws IOException {
		checkWritable();
		try {
			// Merge with existing meta data
			if (keepPreviousMetaData && exists(getWellMetaDataPath())) fillMetaDataMap(metadataMap);
			MDArray<String> metadata = createMDArrayFromMetaDataMap(metadataMap);

			getWriter().writeStringVariableLengthMDArray(getWellMetaDataPath(), metadata, HDF5GenericStorageFeatures.GENERIC_DEFLATE_MAX);
		} catch (Exception e) {
			throw new IOException("Failed to write meta data", e);
		}
	}

	public Map<String, Map<String, String>> getMetaData() {
		Map<String, Map<String, String>> metadataMap = new HashMap<>();
		return fillMetaDataMap(metadataMap);
	}

	/*
	 * *****
	 * Gates
	 * *****
	 */

	public String getGate(String id) throws IOException {
		try {
			String gate = getReader().readString(getGatesPath() + "/" + id);
			return gate;
		} catch (Exception e) {
			throw new IOException("Failed to read gate " + id, e);
		}
	}

	/*
	 * **********
	 * Data paths
	 * **********
	 */

	public static String getWellDataPath() {
		return "/WellData";
	}

	public static String getWellDataPath(String featureId) {
		return getWellDataPath() + "/" + escape(featureId);
	}

	public static String getSubWellDataPath() {
		return "/SubWellData";
	}

	public static String getSubWellDataPath(int wellNr, String featureId) {
		return getSubWellDataPath() + "/" + escape(featureId) + "/" + wellNr;
	}

	public static String getImageDataPath() {
		return "/ImageFiles";
	}

	public static String getExtraDataPath() {
		return "/ExtraFiles";
	}

	public static String getGatesPath() {
		return "/Gates";
	}

	public static String getPlateLayoutPath() {
		return "/PlateLayout";
	}

	public static String getWellMetaDataPath() {
		return "/WellMetaData";
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private static String escape(String featureId) {
		return featureId.replace("/", "%2F");
	}

	private static String unescape(String featureId) {
		return featureId.replace("%2F", "/");
	}

	private MDArray<String> createMDArrayFromMetaDataMap(Map<String, Map<String, String>> metadataMap) {
		// Get all possible meta data columns
		Set<String> keywords = new HashSet<>();
		for (String well : metadataMap.keySet())
			keywords.addAll(metadataMap.get(well).keySet());

		MDArray<String> metadata = new MDArray<>(String.class, new int[] { keywords.size() + 1, metadataMap.keySet().size() + 1 });
		int col = 0;
		int row = 0;
		metadata.set("Wells", col, row);
		for (String column : keywords)
			metadata.set(column, ++col, row);
		for (String wellNr : metadataMap.keySet()) {
			col = 0;
			metadata.set(wellNr, col, ++row);
			for (String column : keywords)
				metadata.set(metadataMap.get(wellNr).get(column) != null ? metadataMap.get(wellNr).get(column) : "", ++col, row);
		}
		return metadata;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Map<String, String>> fillMetaDataMap(Map<String, Map<String, String>> metadataMap) {
		if (!getReader().exists(getWellMetaDataPath())) return metadataMap;
		MDArray<String> metadata = getReader().readStringMDArray(getWellMetaDataPath());
		for (int row = 1; row < metadata.longDimensions()[1]; row++) {
			Map<String, String> datamap = (Map<String, String>) (metadataMap.get(metadata.get(0, row)) != null ? metadataMap.get(metadata.get(0, row)) : new HashMap<>());
			for (int col = 1; col < metadata.longDimensions()[0]; col++)
				if (!datamap.containsKey(metadata.get(col, 0))) datamap.put(metadata.get(col, 0), metadata.get(col, row));
			metadataMap.put(metadata.get(0, row), datamap);
		}
		return metadataMap;
	}

}
