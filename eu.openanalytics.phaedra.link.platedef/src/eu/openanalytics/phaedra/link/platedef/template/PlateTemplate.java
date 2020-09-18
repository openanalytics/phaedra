package eu.openanalytics.phaedra.link.platedef.template;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.model.protocol.vo.WellType;

public class PlateTemplate implements Cloneable {

	private String id;
	private long protocolClassId;
	
	private String creator;
	
	private int rows;
	private int columns;
	
	private Map<Integer, WellTemplate> wells;

	public void fillBlank() {
		fillBlank(8,12);
	}
	
	public void fillBlank(int rows, int columns) {
		id = "New Plate Template";
		wells = new HashMap<Integer, WellTemplate>();
		this.rows = rows;
		this.columns = columns;
		resize();
	}
	
	private void resize() {
		if (wells == null) return;
		for (int nr=1; nr<=getColumns()*getRows(); nr++) {
			WellTemplate wt = wells.get(nr);
			if (wt == null) {
				wt = new WellTemplate();
				wt.setNr(nr);
				wt.setWellType(WellType.EMPTY);
				wells.put(nr, wt);
			}
		}
	}
	
	/*
	 * Getters & setters
	 * *****************
	 */
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getProtocolClassId() {
		return protocolClassId;
	}

	public void setProtocolClassId(long protocolClassId) {
		this.protocolClassId = protocolClassId;
	}

	public String getCreator() {
		return creator;
	}
	
	public void setCreator(String creator) {
		this.creator = creator;
	}
	
	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
		resize();
	}

	public int getColumns() {
		return columns;
	}

	public void setColumns(int columns) {
		this.columns = columns;
		resize();
	}

	public Map<Integer, WellTemplate> getWells() {
		return wells;
	}
	
	public void setWells(Map<Integer, WellTemplate> wells) {
		this.wells = wells;
	}

	@Override
	public String toString() {
		return id;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		PlateTemplate other = (PlateTemplate) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	@Override
	public PlateTemplate clone() throws CloneNotSupportedException {
		PlateTemplate clone = (PlateTemplate)super.clone();
		Map<Integer, WellTemplate> clonedWells = new HashMap<Integer, WellTemplate>();
		for (WellTemplate t: wells.values()) {
			clonedWells.put(t.getNr(), t.clone());
		}
		clone.setWells(clonedWells);
		return clone;
	}
}
