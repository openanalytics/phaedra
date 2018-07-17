package eu.openanalytics.phaedra.silo.util;

import java.util.Arrays;
import java.util.stream.IntStream;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData.SiloDatasetColumnData;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;

public class SiloUtils {

	public static SiloDataset getDataset(Silo silo, String datasetName) {
		return SiloService.streamableList(silo.getDatasets()).stream()
				.filter(ds -> ds.getName().equals(datasetName))
				.findAny().orElse(null);
	}
	
	public static SiloDatasetColumn getColumn(SiloDataset dataset, long columnId) {
		return SiloService.streamableList(dataset.getColumns()).stream()
				.filter(c -> c.getId() == columnId)
				.findAny().orElse(null);
	}
	
	public static Feature getWellFeature(SiloDatasetColumn column) {
		return ProtocolUtils.getFeatureByName(column.getName(), column.getDataset().getSilo().getProtocolClass());
	}
	
	public static SubWellFeature getSubWellFeature(SiloDatasetColumn column) {
		return ProtocolUtils.getSubWellFeatureByName(column.getName(), column.getDataset().getSilo().getProtocolClass());
	}
	
	public static SiloDataType getDataType(IFeature feature) {
		if (feature == null) return SiloDataType.None;
		return feature.isNumeric() ? SiloDataType.Float : SiloDataType.String;
	}
	
	public static SiloDatasetColumnData createColumnData(SiloDatasetColumn column, int size) {
		SiloDatasetColumnData columnData = new SiloDatasetColumnData();
		columnData.setColumn(column);
		switch (column.getType()) {
		case String:
			columnData.setStringData(new String[size]);
			break;
		case Float:
			columnData.setFloatData(new float[size]);
			Arrays.fill(columnData.getFloatData(), Float.NaN);
			break;
		case Long:
			columnData.setLongData(new long[size]);
			break;
		default:
			break;
		}
		return columnData;
	}
	
	public static void replaceColumnData(SiloDatasetColumnData columnData, Object newData, int expectedSize) throws SiloException {
		switch (columnData.getColumn().getType()) {
		case String:
			String[] newStringData = (String[]) newData;
			if (newStringData == null) newStringData = new String[0];
			if (newStringData.length != expectedSize) throw new SiloException(String.format("Data size (%d) doesn't match expected size (%d)", newStringData.length, expectedSize));
			columnData.setStringData(newStringData);
			break;
		case Float:
			float[] newFloatData = (float[]) newData;
			if (newFloatData == null) newFloatData = new float[0];
			if (newFloatData.length != expectedSize) throw new SiloException(String.format("Data size (%d) doesn't match expected size (%d)", newFloatData.length, expectedSize));
			columnData.setFloatData(newFloatData);
			break;
		case Long:
			long[] newLongData = (long[]) newData;
			if (newLongData == null) newLongData = new long[0];
			if (newLongData.length != expectedSize) throw new SiloException(String.format("Data size (%d) doesn't match expected size (%d)", newLongData.length, expectedSize));
			columnData.setLongData(newLongData);
			break;
		default:
			break;
		}
	}
	
	public static void resizeColumnData(SiloDatasetColumnData data, int newSize) {
		switch (data.getColumn().getType()) {
		case String:
			String[] oldStringData = data.getStringData();
			if (oldStringData == null) oldStringData = new String[0];
			if (oldStringData.length == newSize) return;
			String[] newStringData = Arrays.copyOf(oldStringData, newSize);
			data.setStringData(newStringData);
			break;
		case Float:
			float[] oldFloatData = data.getFloatData();
			if (oldFloatData == null) oldFloatData = new float[0];
			if (oldFloatData.length == newSize) return;
			float[] newFloatData = Arrays.copyOf(oldFloatData, newSize);
			if (newFloatData.length > oldFloatData.length) Arrays.fill(newFloatData, oldFloatData.length, newFloatData.length, Float.NaN);
			data.setFloatData(newFloatData);
			break;
		case Long:
			long[] oldLongData = data.getLongData();
			if (oldLongData == null) oldLongData = new long[0];
			if (oldLongData.length == newSize) return;
			long[] newLongData = Arrays.copyOf(oldLongData, newSize);
			data.setLongData(newLongData);
			break;
		default:
			break;
		}
	}
	
	public static void resizeColumnData(SiloDatasetColumnData data, int[] rowsToRemove) {
		if (rowsToRemove != null && rowsToRemove.length == 0) return;
		
		switch (data.getColumn().getType()) {
		case String:
			String[] oldStringData = data.getStringData();
			if (oldStringData == null) oldStringData = new String[0];
			String[] newStringData = null;
			if (oldStringData.length > rowsToRemove.length) {
				final String[] t = oldStringData;
				newStringData = IntStream.range(0, t.length)
					.filter(i -> !CollectionUtils.contains(rowsToRemove, i))
					.mapToObj(i -> t[i])
					.toArray(i -> new String[i]);
			}
			data.setStringData(newStringData);
			break;
		case Float:
			float[] oldFloatData = data.getFloatData();
			if (oldFloatData == null) oldFloatData = new float[0];
			float[] newFloatData = null;
			if (oldFloatData.length > rowsToRemove.length) {
				final float[] t = oldFloatData;
				double[] td = IntStream.range(0, t.length)
					.filter(i -> !CollectionUtils.contains(rowsToRemove, i))
					.mapToDouble(i -> t[i])
					.toArray();
				newFloatData = new float[t.length];
				for (int i = 0; i < t.length; i++) newFloatData[i] = (float) td[i];
			}
			data.setFloatData(newFloatData);
			break;
		case Long:
			long[] oldLongData = data.getLongData();
			if (oldLongData == null) oldLongData = new long[0];
			long[] newLongData = null;
			if (oldLongData.length > rowsToRemove.length) {
				final long[] t = oldLongData;
				newLongData = IntStream.range(0, t.length)
					.filter(i -> !CollectionUtils.contains(rowsToRemove, i))
					.mapToLong(i -> t[i])
					.toArray();
			}
			data.setLongData(newLongData);
			break;
		default:
			break;
		}
	}
	
	public static void saveSiloChanges(Silo original, Silo workingCopy) {
		// Save the silo model
		ObjectCopyFactory.copy(workingCopy, original, false);
		SiloService.getInstance().updateSilo(original);
	}
}
