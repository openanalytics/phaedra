package eu.openanalytics.phaedra.base.hdf5;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.hdf5.HDF5CompoundMemberInformation;
import ch.systemsx.cisd.hdf5.HDF5CompoundType;
import ch.systemsx.cisd.hdf5.HDF5DataClass;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5FloatStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

/**
 * Interface to HDF5 files. Functionality provided:
 * 
 * <ul>
 * <li>Navigation and manipulation of groups</li>
 * <li>Reading and writing attributes: strings, numerical or arrays</li>
 * <li>Reading and writing 1D and 2D data: strings, numerical or binary</li>
 * </ul>
 */
public class BaseHDF5File {

	private String path;

	private boolean readonly;
	private boolean caseSensitive;
	
	private IHDF5Reader reader;
	private IHDF5Writer writer;

	public BaseHDF5File(String path, boolean readonly) {
		this.path = path;
		this.readonly = readonly;
		this.caseSensitive = false;
		open();
	}

	public String getFilePath() {
		return path;
	}

	public void open() {
		// If already open, ignore.
		if (reader != null) return;

		if (readonly) {
			this.reader = HDF5Factory.openForReading(path);
		} else {
			this.writer = HDF5Factory.open(path);
			this.reader = writer;
		}

		HDF5FileRegistry.add(this);
	}

	public void close() {
		if (writer != null) {
			try {
				writer.flushSyncBlocking();
			} catch (Throwable t) {
				// Ignore errors if file is already closed.
			}
		}
		if (reader != null) reader.close();
		reader = null;
		writer = null;

		HDF5FileRegistry.remove(this);
	}

	/*
	 * **********
	 * Navigation
	 * **********
	 */

	public boolean hasChildren(String path) {
		return reader.isGroup(resolvePath(path));
	}

	public String[] getChildren(String path) throws IOException {
		List<String> members = reader.getGroupMembers(resolvePath(path));
		return members.toArray(new String[members.size()]);
	}

	public void createGroup(String path, String name) throws IOException {
		if (!path.endsWith("/")) path += "/";
		createGroup(path + name);
	}

	public void createGroup(String path) throws IOException {
		checkWritable();
		writer.createGroup(path);
	}

	public boolean exists(String path) {
		String resolvedPath = resolvePath(path);
		if (resolvedPath == null) return false;
		return reader.exists(resolvedPath);
	}

	public boolean isDataNumeric(String path) {
		return isNumeric(getDatasetClass(path));
	}

	public boolean isDataset(String path) {
		return reader.isDataSet(resolvePath(path));
	}

	public boolean isCompoundDataSet(String path) {
		return reader.getDataSetInformation(resolvePath(path)).getTypeInformation().getDataClass() == HDF5DataClass.COMPOUND;
	}

	public boolean isGroup(String path) {
		return reader.isGroup(resolvePath(path));
	}

	public long[] getDataDimensions(String path) {
		HDF5DataSetInformation info = reader.getDataSetInformation(resolvePath(path));
		if (info == null) return new long[0];
		return info.getDimensions();
	}
	
	/*
	 * **********
	 * Attributes
	 * **********
	 */

	public boolean existsAttribute(String path, String name) {
		return reader.hasAttribute(resolvePath(path), name);
	}

	public boolean isAttributeNumeric(String path, String name) {
		return isNumeric(getAttributeClass(path, name));
	}

	public String[] getAttributes(String path) {
		List<String> attributes = reader.getAllAttributeNames(resolvePath(path));
		return attributes.toArray(new String[attributes.size()]);
	}

	public Object getAttribute(String path, String name) {
		if (!existsAttribute(path,name)) return null;
		Class<?> clazz = getAttributeClass(path, name);
		path = resolvePath(path);
		if (clazz == float.class) {
			return reader.getFloatAttribute(path, name);
		} else if (clazz == double.class) {
			return reader.getDoubleAttribute(path, name);
		} else if (clazz == int.class) {
			return reader.getIntAttribute(path, name);
		} else if (clazz == short.class) {
			return reader.getShortAttribute(path, name);
		} else if (clazz == long.class) {
			return reader.getLongAttribute(path, name);
		} else if (clazz == byte.class || clazz == String.class) {
			return reader.getStringAttribute(path, name);
		} else if (clazz == float[].class) {
			return reader.getFloatArrayAttribute(path, name);
		} else if (clazz == String[].class) {
			return reader.getStringArrayAttribute(path, name);
		}
		return null;
	}

	public float[] getAttributeFloatArray(String path, String name) {
		return (float[])getAttribute(path, name);
	}

	public String[] getAttributeStringArray(String path, String name) {
		return (String[])getAttribute(path, name);
	}

	public void setAttribute(String path, String key, Object value) throws IOException {
		checkWritable();
		try {
			if (value instanceof Float) {
				writer.setFloatAttribute(path, key, (Float)value);
			} else if (value instanceof Double) {
				writer.setDoubleAttribute(path, key, (Double)value);
			} else if (value instanceof Short) {
				writer.setShortAttribute(path, key, (Short)value);
			} else if (value instanceof Integer) {
				writer.setIntAttribute(path, key, (Integer)value);
			} else if (value instanceof Long) {
				writer.setLongAttribute(path, key, (Long)value);
			} else if (value instanceof float[]) {
				writer.setFloatArrayAttribute(path, key, (float[])value);
			} else if (value instanceof String) {
				writer.setStringAttributeVariableLength(path, key, (String)value);
			} else if (value instanceof String[]) {
				writer.setStringArrayAttribute(path, key, (String[])value);
			}
		} catch (Exception e) {
			throw new IOException("Failed to write attribute at " + path, e);
		}
	}

	/*
	 * ********
	 * Datasets
	 * ********
	 */

	public String[] getStringData1D(String path) throws IOException {
		return getStringData1D(path, 1);
	}

	public String[] getStringData1D(String path, int stride) throws IOException {
		try {
			path = resolvePath(path);
			if (stride > 1) return (String[]) getHyperslab(stride, path, HDF5Constants.H5T_STRING);
			else return reader.readStringArray(path);
		} catch (Exception e) {
			throw new IOException("Failed to read data " + path, e);
		}
	}

	public void writeStringData1D(String path, String[] data) throws IOException {
		checkWritable();
		try {
			writer.writeStringVariableLengthArray(path, data, HDF5GenericStorageFeatures.GENERIC_DEFLATE_MAX);
		} catch (Exception e) {
			throw new IOException("Failed to write data " + path, e);
		}
	}

	public float[] getNumericData1D(String path) throws IOException {
		return getNumericData1D(path, 1);
	}

	/**
	 * Returns a 1D Array (String, float, double, short, int, long and byte).
	 * @param path
	 * @param stride
	 * @return An array.
	 * @throws IOException
	 */
	public Object getAnyData1D(String path, int stride) throws IOException {
		try {
			path = resolvePath(path);
			boolean useHyperslab = stride > 1;
			Class<?> dataClass = getDatasetClass(path);
			if (dataClass == float.class) {
				if (useHyperslab) return getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_FLOAT);
				else return reader.readFloatArray(path);
			} else if (dataClass == double.class) {
				if (useHyperslab) return getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_DOUBLE);
				else return reader.readDoubleArray(path);
			} else if (dataClass == short.class) {
				if (useHyperslab) return getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_SHORT);
				else return reader.readShortArray(path);
			} else if (dataClass == int.class) {
				if (useHyperslab) return getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_INT);
				else return reader.readIntArray(path);
			} else if (dataClass == long.class) {
				if (useHyperslab) return getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_LONG);
				else return reader.readLongArray(path);
			} else if (dataClass == byte.class) {
				if (useHyperslab) return getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_CHAR);
				else return reader.readByteArray(path);
			} else if (dataClass == String.class) {
				if (useHyperslab) return getHyperslab(stride, path, HDF5Constants.H5T_STRING);
				else return reader.readStringArray(path);
			}
			throw new IOException("Unsupported data class " + dataClass + " for data " + path);
		} catch (Exception e) {
			throw new IOException("Failed to read data " + path, e);
		}
	}

	public float[] getNumericData1D(String path, int stride) throws IOException {
		try {
			path = resolvePath(path);
			boolean useHyperslab = stride > 1;
			Class<?> dataClass = getDatasetClass(path);
			if (dataClass == float.class) {
				if (useHyperslab) return (float[]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_FLOAT);
				else return reader.readFloatArray(path);
			} else if (dataClass == double.class) {
				double[] values = null;
				if (useHyperslab) values = (double[]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_DOUBLE);
				else values = reader.readDoubleArray(path);
				return convertToFloat1D(values);
			} else if (dataClass == short.class) {
				short[] values = null;
				if (useHyperslab) values = (short[]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_SHORT);
				else values = reader.readShortArray(path);
				return convertToFloat1D(values);
			} else if (dataClass == int.class) {
				int[] values = null;
				if (useHyperslab) values = (int[]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_INT);
				else values = reader.readIntArray(path);
				return convertToFloat1D(values);
			} else if (dataClass == long.class) {
				long[] values = null;
				if (useHyperslab) values = (long[]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_LONG);
				else values = reader.readLongArray(path);
				return convertToFloat1D(values);
			} else if (dataClass == byte.class) {
				byte[] values = null;
				if (useHyperslab) values = (byte[]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_CHAR);
				else values = reader.readByteArray(path);
				return convertToFloat1D(values);
			}
			throw new IOException("Unsupported data class " + dataClass + " for data " + path);
		} catch (Exception e) {
			throw new IOException("Failed to read data " + path, e);
		}
	}

	public float[][] getNumericData2D(String path) throws IOException {
		return getNumericData2D(path, 1);
	}

	public float[][] getNumericData2D(String path, int stride) throws IOException {
		try {
			path = resolvePath(path);
			boolean useHyperslab = stride > 1;
			Class<?> dataClass = getDatasetClass(path);
			if (dataClass == float.class) {
				if (useHyperslab) return (float[][]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_FLOAT);
				else return reader.readFloatMatrix(path);
			} else if (dataClass == double.class) {
				double[][] values = null;
				if (useHyperslab) values = (double[][]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_DOUBLE);
				else values = reader.readDoubleMatrix(path);
				return convertToFloat2D(values);
			} else if (dataClass == short.class) {
				short[][] values = null;
				if (useHyperslab) values = (short[][]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_SHORT);
				else values = reader.readShortMatrix(path);
				return convertToFloat2D(values);
			} else if (dataClass == int.class) {
				int[][] values = null;
				if (useHyperslab) values = (int[][]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_INT);
				else values = reader.readIntMatrix(path);
				return convertToFloat2D(values);
			} else if (dataClass == long.class) {
				long[][] values = null;
				if (useHyperslab) values = (long[][]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_LONG);
				else values = reader.readLongMatrix(path);
				return convertToFloat2D(values);
			} else if (dataClass == byte.class) {
				byte[][] values = null;
				if (useHyperslab) values = (byte[][]) getHyperslab(stride, path, HDF5Constants.H5T_NATIVE_CHAR);
				else values = reader.readByteMatrix(path);
				return convertToFloat2D(values);
			}
			throw new IOException("Unsupported data class " + dataClass + " for data " + path);
		} catch (Exception e) {
			throw new IOException("Failed to read data " + path, e);
		}
	}

	public void writeNumericData(String path, float[] data) throws IOException {
		checkWritable();
		try {
			writer.writeFloatArray(path, data, HDF5FloatStorageFeatures.FLOAT_DEFLATE_MAX);
		} catch (Exception e) {
			throw new IOException("Failed to write data " + path, e);
		}
	}
	
	public void writeNumericData(String path, double[] data) throws IOException {
		checkWritable();
		try {
			writer.writeDoubleArray(path, data, HDF5FloatStorageFeatures.FLOAT_DEFLATE_MAX);
		} catch (Exception e) {
			throw new IOException("Failed to write data " + path, e);
		}
	}

	public void writeNumericData(String path, int[] data) throws IOException {
		checkWritable();
		try {
			writer.writeIntArray(path, data, HDF5IntStorageFeatures.INT_DEFLATE_MAX);
		} catch (Exception e) {
			throw new IOException("Failed to write data " + path, e);
		}
	}

	public void writeNumericData(String path, long[] data) throws IOException {
		checkWritable();
		try {
			writer.writeLongArray(path, data, HDF5IntStorageFeatures.INT_DEFLATE_MAX);
		} catch (Exception e) {
			throw new IOException("Failed to write data " + path, e);
		}
	}

	public void writeNumericData(String path, float[][] data) throws IOException {
		checkWritable();
		try {
			writer.writeFloatMatrix(path, data, HDF5FloatStorageFeatures.FLOAT_DEFLATE_MAX);
		} catch (Exception e) {
			throw new IOException("Failed to write data " + path, e);
		}
	}

	public InputStream getBinaryData(String path) throws IOException {
		try {
			path = resolvePath(path);
			long size = reader.getSize(path);
			if (size >= Integer.MAX_VALUE) {
				// Use blocks to transfer contents.
				String tempFile = FileUtils.generateTempFolder(true) + "/binary.dat";
				try (OutputStream out = new FileOutputStream(tempFile)) {
					int blockSize = 1024*1024*10;
					long blockCount = (size / blockSize);
					int remainder = (int)(size - (blockCount*blockSize)); 

					for (long blockNr = 0; blockNr < blockCount; blockNr++) {
						byte[] block = reader.readAsByteArrayBlock(path, blockSize, blockNr);
						out.write(block);
					}
					if (remainder > 0) {
						byte[] block = reader.readAsByteArrayBlockWithOffset(path, remainder, blockCount*blockSize);
						out.write(block);
					}
				}
				return new FileInputStream(tempFile);
			} else {
				byte[] bytes = reader.readAsByteArray(path);
				return new ByteArrayInputStream(bytes);
			}
		} catch (Exception e) {
			throw new IOException("Failed to read data " + path, e);
		}
	}

	public void writeBinaryData(String path, byte[] data) throws IOException {
		InputStream stream = new ByteArrayInputStream(data);
		long size = data.length;
		writeBinaryData(path, stream, size);
	}

	public void writeBinaryData(String path, InputStream data, long size) throws IOException {
		checkWritable();
		try {
			int blockSize = 1024 * 1024 * 5;
			if (blockSize > size) blockSize = (int) size;
			long blockNr = 0;
			writer.createByteArray(path, size, blockSize, HDF5IntStorageFeatures.INT_DEFLATE_MAX);

			byte[] block = StreamUtils.readBlock(data, blockSize);
			while (block != null) {
				long offset = blockSize * blockNr++;
				writer.writeByteArrayBlockWithOffset(path, block, block.length, offset);
				block = StreamUtils.readBlock(data, blockSize);
			}
		} catch (Exception e) {
			throw new IOException("Failed to write data " + path, e);
		}
	}

	public Object[][] getCompoundData(String path, List<String> fieldNames) throws IOException {
		try {
			path = resolvePath(path);
			Class<?> dataClass = getDatasetClass(path);
			if (dataClass == Map.class) {
				if (fieldNames != null) {
					HDF5CompoundMemberInformation[] infos = reader.compounds().getDataSetInfo(path);
					for (HDF5CompoundMemberInformation info: infos) {
						fieldNames.add(info.getName());
					}
				}
				Object[][] values = reader.compounds().readArray(path, Object[].class);
				return values;
			}
			throw new IOException("Unsupported data class " + dataClass + " for data " + path);
		} catch (Exception e) {
			throw new IOException("Failed to read data " + path, e);
		}
	}

	public Object[][] getCompoundDataBlock(String path, List<String> fieldNames, int rows, long start) throws IOException {
		try {
			path = resolvePath(path);
			Class<?> dataClass = getDatasetClass(path);
			if (dataClass == Map.class) {
				if (fieldNames != null) {
					HDF5CompoundMemberInformation[] infos = reader.compounds().getDataSetInfo(path);
					for (HDF5CompoundMemberInformation info: infos) {
						fieldNames.add(info.getName());
					}
				}
				final HDF5CompoundType<Object[]> type = reader.compounds().getDataSetType(path, Object[].class);
				Object[][] values = reader.compounds().readArrayBlockWithOffset(path, type, rows, start);
				return values;
			}
			throw new IOException("Unsupported data class " + dataClass + " for data " + path);
		} catch (Exception e) {
			throw new IOException("Failed to read data " + path, e);
		}
	}

	public void writeCompoundData(String path, String[] columnNames, Object[][] data) throws IOException {
		writeCompoundData(path, columnNames, data, false);
	}

	/**
	 * Write a 2D Object Array to the HDF5 file as a Table.
	 * The method will try to determine which Java Array types are included. If a column exists only of 'null' an error is thrown.
	 * 
	 * @param path The location inside the HDF5 file.
	 * @param columnNames The column names.
	 * @param data The 2D Object Array that should be written to the HDF5 File.
	 * @throws IOException
	 */
	public void writeCompoundData(String path, String[] columnNames, Object[][] data, boolean overwrite) throws IOException {
		int columns = columnNames.length;
		Object[] dataTypes = new Object[columns];

		for (int col = 0; col < columns; col++) {
			Object colSample = null;
			// Keep looping the rows in a column till a value is found.
			int row = 0;
			for ( ; row < data.length; row++) {
				colSample = data[row][col];
				if (colSample != null) break;
			}
			if (colSample == null) {
				throw new IOException("Unsupported data class 'null' for column " + columnNames[col] + " in data " + path);
			}
			if (colSample instanceof Number) {
				if (colSample instanceof Long) {
					dataTypes[col] = Long.MAX_VALUE;
				} else if (colSample instanceof Integer) {
					dataTypes[col] = Integer.MAX_VALUE;
				} else {
					dataTypes[col] = Float.NaN;
				}
			}
			if (colSample instanceof String) {
				String longest = colSample.toString();
				for ( ; row < data.length; row++) {
					colSample = data[row][col];
					if (colSample != null) {
						String newString = colSample.toString();
						if (newString.length() > longest.length()) {
							longest = newString;
						}
					}
				}
				dataTypes[col] = longest;
			}
		}

		writeCompoundData(path, columnNames, dataTypes, data, overwrite);
	}

	public void writeCompoundData(String path, String[] columnNames, Object[] dataTypes, Object[][] data) throws IOException {
		writeCompoundData(path, columnNames, dataTypes, data, false);
	}

	/**
	 * Write a 2D Object Array to the HDF5 file as a Table.
	 * 
	 * @param path The location inside the HDF5 file.
	 * @param columnNames The column names.
	 * @param dataTypes The column data types.
	 * @param data The 2D Object Array that should be written to the HDF5 File.
	 * @param overwrite Force overwriting.
	 * @throws IOException
	 */
	public void writeCompoundData(String path, String[] columnNames, Object[] dataTypes, Object[][] data, boolean overwrite) throws IOException {
		try {
			// Create the column data types.
			HDF5CompoundType<Object[]> inferredType = writer.compounds().getInferredType(columnNames, dataTypes);
			// Check if the Compound Table already exists.
			if (!writer.exists(path)) {
				// Compound Table does not exist yet. Create it.
				writer.compounds().createArray(path, inferredType, 0, HDF5GenericStorageFeatures.GENERIC_CHUNKED_KEEP);
				writer.compounds().writeArray(path, inferredType, data, HDF5GenericStorageFeatures.GENERIC_CHUNKED_KEEP);
			} else {
				// Compound Table does exist. See if the same columns are present.
				HDF5CompoundMemberInformation[] dataSetInfos = reader.compounds().getDataSetInfo(path);
				HDF5CompoundMemberInformation[] newDataSetInfos = inferredType.getCompoundMemberInformation();
				if (overwrite || !isIdenticalDataTypes(dataSetInfos, newDataSetInfos)) {
					// Delete previous compound & overwrite existing data.
					writer.delete(path);
					writer.compounds().createArray(path, inferredType, 0, HDF5GenericStorageFeatures.GENERIC_CHUNKED_KEEP);
					writer.compounds().writeArray(path, inferredType, data, HDF5GenericStorageFeatures.GENERIC_CHUNKED_KEEP);
					// TODO: Previous Compound Types remain in the HDF5 file. Find a proper way to delete them.
				} else {
					// Get the size for the offset.
					long[] dims = getDataDimensions(path);
					writer.compounds().writeArrayBlockWithOffset(path, inferredType, data, dims[0]);
				}
			}
			writer.flush();
		} catch (Exception e) {
			throw new IOException("Failed to write data " + path, e);
		}
	}

	public void writeObjectReference(String objectPath, String referencedObjectPath) throws IOException {
		checkWritable();
		try {
			writer.writeObjectReference(objectPath, referencedObjectPath);
		} catch (Exception e) {
			throw new IOException("Failed to create reference", e);
		}
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	protected IHDF5Reader getReader() {
		return reader;
	}

	protected IHDF5Writer getWriter() {
		return writer;
	}

	protected void checkWritable() throws IOException {
		if (readonly) throw new IOException("File is opened for read-only");
	}

	protected Class<?> getDatasetClass(String path) {
		HDF5DataSetInformation info = reader.getDataSetInformation(resolvePath(path));
		Class<?> dataClass = info.getTypeInformation().tryGetJavaType();
		return dataClass;
	}
	
	protected Class<?> getAttributeClass(String path, String name) {
		HDF5DataTypeInformation info = reader.getAttributeInformation(resolvePath(path), name);
		return info.tryGetJavaType();
	}
	
	protected boolean isNumeric(Class<?> clazz) {
		// Note: bytes are considered numeric (0 - 255)
		return (clazz == float.class || clazz == double.class || clazz == short.class || clazz == int.class || clazz == long.class || clazz == byte.class);
	}

	protected String resolvePath(String path) {
		if (reader.exists(path)) {
			// Resolve references, if any.
			if (reader.isDataSet(path)) {
				HDF5DataClass dataClass = reader.getDataSetInformation(path).getTypeInformation().getDataClass();
				if (dataClass.toString().equals("REFERENCE")) return reader.readObjectReference(path);
			}
			return path;
		} else if (!caseSensitive) {
			// Look for a match with a different case.
			String[] pathParts = path.split("/");
			String resolvedPath = "/";
			for (int i = 1; i < pathParts.length; i++) {
				String testPath = (resolvedPath.endsWith("/")) ? resolvedPath + pathParts[i] : resolvedPath + "/" + pathParts[i];
				if (reader.exists(testPath)) resolvedPath = testPath;
				else if (reader.isGroup(resolvedPath)) {
					List<String> children = reader.getGroupMembers(resolvedPath);
					for (String child: children) {
						if (child.equalsIgnoreCase(pathParts[i])) resolvedPath = resolvedPath + "/" + child;
					}
				}
			}
			if (path.equalsIgnoreCase(resolvedPath)) return resolvedPath;
		}
		return null;
	}

	private float[] convertToFloat1D(Object array) throws IOException {
		if (array instanceof float[]) {
			return (float[])array;
		} else if (array instanceof double[]) {
			double[] values = (double[])array;
			float[] floatValues = new float[values.length];
			for (int i = 0; i < values.length; i++) floatValues[i] = (float)values[i];
			return floatValues;
		} else if (array instanceof short[]) {
			short[] values = (short[])array;
			float[] floatValues = new float[values.length];
			for (int i = 0; i < values.length; i++) floatValues[i] = values[i];
			return floatValues;
		} else if (array instanceof int[]) {
			int[] values = (int[])array;
			float[] floatValues = new float[values.length];
			for (int i = 0; i < values.length; i++) floatValues[i] = values[i];
			return floatValues;
		} else if (array instanceof long[]) {
			long[] values = (long[])array;
			float[] floatValues = new float[values.length];
			for (int i = 0; i < values.length; i++) floatValues[i] = values[i];
			return floatValues;
		} else if (array instanceof byte[]) {
			byte[] values = (byte[])array;
			float[] floatValues = new float[values.length];
			for (int i = 0; i < values.length; i++) floatValues[i] = values[i];
			return floatValues;
		}
		throw new IOException("Cannot convert to float array: " + array.getClass());
	}

	private float[][] convertToFloat2D(Object array) throws IOException {
		if (array instanceof float[][]) {
			return (float[][])array;
		} else if (array instanceof double[][]) {
			double[][] values = (double[][])array;
			float[][] floatValues = new float[values.length][];
			for (int i = 0; i < values.length; i++) {
				floatValues[i] = new float[values[i].length];
				for (int j = 0; j < values[i].length; j++) {
					floatValues[i][j] = (float)values[i][j];
				}
			}
			return floatValues;
		} else if (array instanceof short[][]) {
			short[][] values = (short[][])array;
			float[][] floatValues = new float[values.length][];
			for (int i = 0; i < values.length; i++) {
				floatValues[i] = new float[values[i].length];
				for (int j = 0; j < values[i].length; j++) {
					floatValues[i][j] = values[i][j];
				}
			}
			return floatValues;
		} else if (array instanceof int[][]) {
			int[][] values = (int[][])array;
			float[][] floatValues = new float[values.length][];
			for (int i = 0; i < values.length; i++) {
				floatValues[i] = new float[values[i].length];
				for (int j = 0; j < values[i].length; j++) {
					floatValues[i][j] = values[i][j];
				}
			}
			return floatValues;
		} else if (array instanceof long[][]) {
			long[][] values = (long[][])array;
			float[][] floatValues = new float[values.length][];
			for (int i = 0; i < values.length; i++) {
				floatValues[i] = new float[values[i].length];
				for (int j = 0; j < values[i].length; j++) {
					floatValues[i][j] = values[i][j];
				}
			}
			return floatValues;
		} else if (array instanceof byte[][]) {
			byte[][] values = (byte[][])array;
			float[][] floatValues = new float[values.length][];
			for (int i = 0; i < values.length; i++) {
				floatValues[i] = new float[values[i].length];
				for (int j = 0; j < values[i].length; j++) {
					floatValues[i][j] = values[i][j];
				}
			}
			return floatValues;
		}
		throw new IOException("Cannot convert to float array: " + array.getClass());
	}

	public void createImage(String path, int[] dimensions) throws IOException {
		checkWritable();
		try {
			// Using a byte array because this generates a uint8 dataset.
			MDByteArray array = new MDByteArray(dimensions);
			writer.writeByteMDArray(path, array, HDF5IntStorageFeatures.INT_DEFLATE_MAX_UNSIGNED);
		} catch (Exception e) {
			throw new IOException("Failed to create reference", e);
		}
	}

	public void writeImageSlice(String path, int c, int t, int[][] data) throws IOException {
		checkWritable();
		if (!exists(path)) throw new IOException("Cannot write image slice: path " + path + " does not exist");

		int dataset_id = -1;
		int dataspace_id = -1;
		int file_id = -1;
		int memspace_id = -1;
		int status;

		try {
			file_id = H5.H5Fopen(this.path, HDF5Constants.H5F_ACC_RDWR, HDF5Constants.H5P_DEFAULT);
			if (file_id < 0) throw new IOException("Failed to open HDF5 file " + this.path);

			dataset_id = H5.H5Dopen(file_id, path, HDF5Constants.H5P_DEFAULT);
			if (dataset_id < 0) throw new RuntimeException("Failed to read dataset " + path);

			dataspace_id = H5.H5Dget_space(dataset_id);
			if (dataspace_id < 0) throw new RuntimeException("Failed to open dataspace for " + path);

			//Note: This code is geared for 4 dimensions

			long[] dataSize = new long[4];
			H5.H5Sget_simple_extent_dims(dataspace_id, dataSize, null);

			//TODO Order in file is assumed to be: c, t, x, y

			// Select a hyperslab on the 4D image
			long[] offset = new long[] { c,t,0,0 };
			long[] stride = new long[] { 1,1,1,1 };
			long[] count = new long[] { 1,1,1,1 };
			long[] block = new long[] { 1,1,dataSize[2],dataSize[3] };

			status = H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, offset, stride, count, block);
			if (status < 0) throw new RuntimeException("Failed to select hyperslab.");

			// Read the hyperslab into a 2D memory plane
			long[] memDims = new long[] { dataSize[2], dataSize[3] };
			memspace_id = H5.H5Screate_simple(2, memDims, null);

			int dataType = HDF5Constants.H5T_NATIVE_INT;

			status = H5.H5Dwrite(dataset_id, dataType, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, data);
			if (status < 0) throw new RuntimeException("Failed to get data.");
		} finally {
			if (file_id >= 0)
				status = H5.H5Fclose(file_id);
			if (dataset_id >= 0)
				status = H5.H5Dclose(dataset_id);
			if (dataspace_id >= 0)
				status = H5.H5Sclose(dataspace_id);
			if (memspace_id >= 0)
				status = H5.H5Sclose(memspace_id);
		}
	}

	public int[][] getImageSlice(String path, int c, int t, long[] stride) throws IOException {
		return getImageSlice(path, 
				new long[]{c,t,0,0},
				new long[]{1,1,-1,-1},
				stride);
	}

	public int[][] getImageSlice(String path, long[] offset, long[] count, long[] stride) throws IOException {

		int dataset_id = -1;
		int dataspace_id = -1;
		int file_id = -1;
		int memspace_id = -1;
		int status;

		try {
			file_id = H5.H5Fopen(this.path, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);
			if (file_id < 0) throw new IOException("Failed to open HDF5 file " + this.path);

			dataset_id = H5.H5Dopen(file_id, path, HDF5Constants.H5P_DEFAULT);
			if (dataset_id < 0) throw new RuntimeException("Failed to read dataset " + path);

			dataspace_id = H5.H5Dget_space(dataset_id);
			if (dataspace_id < 0) throw new RuntimeException("Failed to open dataspace for " + path);

			//Note: This code is geared for 4 dimensions

			long[] dataSize = new long[4];
			H5.H5Sget_simple_extent_dims(dataspace_id, dataSize, null);

			//TODO Order in file is assumed to be: c, t, x, y

			// Select a hyperslab on the 4D image
			if (offset == null) offset = new long[]{0,0,0,0};
			if (stride == null) stride = new long[] { 1,1,1,1 };
			if (count == null) count = new long[] { dataSize[0]/stride[0],dataSize[1]/stride[1],dataSize[2]/stride[2],dataSize[3]/stride[3] };
			long[] block = new long[] { 1,1,1,1 };
			long[] originalSize = new long[] {count[0],count[1],count[2],count[3]};

			int fullDimCount = 0;
			for (int i=0; i<4; i++) if (offset[i] < 0) offset[i] = 0;
			for (int i=0; i<4; i++) if (originalSize[i] == -1) originalSize[i] = dataSize[i]/stride[i] - offset[i];
			for (int i=0; i<4; i++) {
				long remainingRange = dataSize[i]/stride[i] - offset[i];
				// Replace -1 with entire range
				if (count[i] == -1) count[i] = remainingRange;
				if (count[i] > remainingRange) count[i] = remainingRange;
				if (count[i] != 1) fullDimCount++;
			}

			if (fullDimCount > 2) {
				throw new RuntimeException("getImageSlice can only return 2D images");
			}

			int dimX=2;
			int dimY=3;

			if (fullDimCount == 2) {
				// Look for the 2 non-singleton dimensions.
				for (dimX=0; dimX<4 ;dimX++) {
					if (count[dimX] != 1) break;
				}
				for (dimY=dimX+1; dimY<4 ;dimY++) {
					if (count[dimY] != 1) break;
				}
			}

			status = H5.H5Sselect_hyperslab(dataspace_id, HDF5Constants.H5S_SELECT_SET, offset, stride, count, block);
			if (status < 0) throw new RuntimeException("Failed to select hyperslab.");

			// Read the hyperslab into a 2D memory plane
			long[] memDims = new long[] { count[dimX], count[dimY] };
			memspace_id = H5.H5Screate_simple(2, memDims, null);

			int dataType = HDF5Constants.H5T_NATIVE_INT;
			int[][] resultset = new int[(int)originalSize[dimX]][(int)originalSize[dimY]];

			status = H5.H5Dread(dataset_id, dataType, memspace_id, dataspace_id, HDF5Constants.H5P_DEFAULT, resultset);
			if (status < 0) throw new RuntimeException("Failed to get data.");

			return resultset;
		} finally {
			if (file_id >= 0)
				status = H5.H5Fclose(file_id);
			if (dataset_id >= 0)
				status = H5.H5Dclose(dataset_id);
			if (dataspace_id >= 0)
				status = H5.H5Sclose(dataspace_id);
			if (memspace_id >= 0)
				status = H5.H5Sclose(memspace_id);
		}
	}

	private Object getHyperslab(int strideNumber, String dataset, int dataType) throws IOException {
		// Using the low-level (non-OO) HDF5 API here to work with hyperslabs.
		//TODO Support reading of 2D datasets.

		int dataset_id = -1;
		int dataspace_id = -1;
		int file_id = -1;
		int memspace = -1;
		int status;

		int startNumber = 0;
		String filePath = path;

		try {
			file_id = H5.H5Fopen(filePath, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);
			if (file_id < 0) {
				throw new IOException("Failed to read file.");
			}
			dataset_id = H5.H5Dopen(file_id, dataset, HDF5Constants.H5P_DEFAULT);
			if (dataset_id < 0) {
				throw new RuntimeException("Failed to dataset.");
			}
			dataspace_id = H5.H5Dget_space(dataset_id);
			if (dataspace_id < 0) {
				throw new RuntimeException("Failed to dataspace.");
			}

			int size = (int) H5.H5Sget_select_npoints(dataspace_id);
			strideNumber = (strideNumber > size) ? size : strideNumber;
			int sizeStrider = Math.round(size / strideNumber);

			long[] start = new long[] { startNumber };
			long[] stride = new long[] { strideNumber };
			long[] count = new long[] { sizeStrider };

			status = H5.H5Sselect_hyperslab(dataspace_id,
					HDF5Constants.H5S_SELECT_SET, start, stride, count, null);
			if (status < 0) {
				throw new RuntimeException("Failed to select hyperslab.");
			}

			long[] dimsm = new long[] { sizeStrider };
			memspace = H5.H5Screate_simple(1, dimsm, null);

			Object rdset_data = null;
			if (dataType == HDF5Constants.H5T_NATIVE_FLOAT) {
				rdset_data = new float[sizeStrider];
			} else if (dataType == HDF5Constants.H5T_NATIVE_DOUBLE) {
				rdset_data = new double[sizeStrider];
			} else if (dataType == HDF5Constants.H5T_NATIVE_INT) {
				rdset_data = new int[sizeStrider];
			} else if (dataType == HDF5Constants.H5T_NATIVE_CHAR) {
				rdset_data = new byte[sizeStrider];
			} else {
				rdset_data = new String[sizeStrider];
			}

			status = H5.H5Dread(dataset_id, dataType, memspace, dataspace_id,
					HDF5Constants.H5P_DEFAULT, rdset_data);
			if (status < 0) {
				throw new RuntimeException("Failed to get data.");
			}

			return rdset_data;
		} finally {
			if (file_id >= 0)
				status = H5.H5Fclose(file_id);
			if (dataset_id >= 0)
				status = H5.H5Dclose(dataset_id);
			if (dataspace_id >= 0)
				status = H5.H5Sclose(dataspace_id);
			if (memspace >= 0)
				status = H5.H5Sclose(memspace);
		}
	}

	private boolean isIdenticalDataTypes(HDF5CompoundMemberInformation[] dataSetInfos, HDF5CompoundMemberInformation[] newDataSetInfos) {
		boolean isIdentical = dataSetInfos.length == newDataSetInfos.length;
		if (isIdentical) {
			for (int i = 0; i < dataSetInfos.length; i++) {
				if (!dataSetInfos[i].equals(newDataSetInfos[i])) {
					isIdentical = false;
					break;
				}
			}
		}
		return isIdentical;
	}

}
