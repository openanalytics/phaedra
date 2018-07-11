package eu.openanalytics.phaedra.silo.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;

public class SiloDatasetData {

	private SiloDataset dataset;
	private SiloDatapoint[] dataPoints;
	private Map<String, SiloDatasetColumnData> columnData;
	
	public SiloDatasetData() {
		columnData = new HashMap<>();
	}
	
	public SiloDataset getDataset() {
		return dataset;
	}

	public void setDataset(SiloDataset dataset) {
		this.dataset = dataset;
	}

	public SiloDatapoint[] getDataPoints() {
		return dataPoints;
	}

	public void setDataPoints(SiloDatapoint[] dataPoints) {
		this.dataPoints = dataPoints;
	}

	public Map<String, SiloDatasetColumnData> getColumnData() {
		return columnData;
	}

	public void setColumnData(Map<String, SiloDatasetColumnData> columnData) {
		this.columnData = columnData;
	}

	public SiloDatasetData copy() {
		SiloDatasetData copy = new SiloDatasetData();
		copy.setDataset(dataset);
		
		SiloDatapoint[] points = new SiloDatapoint[dataPoints.length];
		for (int i = 0; i < points.length; i++) {
			points[i] = new SiloDatapoint();
			points[i].setDatapointId(dataPoints[i].getDatapointId());
			points[i].setSubwellId(dataPoints[i].getSubwellId());
			points[i].setWellId(dataPoints[i].getWellId());
		}
		copy.setDataPoints(points);
		
		for (Entry<String, SiloDatasetColumnData> entry: columnData.entrySet()) {
			SiloDatasetColumnData column = new SiloDatasetColumnData();
			column.setColumn(entry.getValue().getColumn());
			column.setStringData(entry.getValue().getStringData());
			column.setFloatData(entry.getValue().getFloatData());
			column.setLongData(entry.getValue().getLongData());
			copy.getColumnData().put(entry.getKey(), column);
		}
		
		return copy;
	}
	
	public static class SiloDatapoint {
		
		private long datapointId;
		private long wellId;
		private long subwellId;
		
		public long getDatapointId() {
			return datapointId;
		}
		public void setDatapointId(long datapointId) {
			this.datapointId = datapointId;
		}
		public long getWellId() {
			return wellId;
		}
		public void setWellId(long wellId) {
			this.wellId = wellId;
		}
		public long getSubwellId() {
			return subwellId;
		}
		public void setSubwellId(long subwellId) {
			this.subwellId = subwellId;
		}
	}
	
	public static class SiloDatasetColumnData {
		
		private SiloDatasetColumn column;
		
		private String[] stringData;
		private float[] floatData;
		private long[] longData;

		public SiloDatasetColumn getColumn() {
			return column;
		}
		public void setColumn(SiloDatasetColumn column) {
			this.column = column;
		}
		public String[] getStringData() {
			return stringData;
		}
		public void setStringData(String[] stringData) {
			this.stringData = stringData;
		}
		public float[] getFloatData() {
			return floatData;
		}
		public void setFloatData(float[] floatData) {
			this.floatData = floatData;
		}
		public long[] getLongData() {
			return longData;
		}
		public void setLongData(long[] longData) {
			this.longData = longData;
		}
	}
	
}
