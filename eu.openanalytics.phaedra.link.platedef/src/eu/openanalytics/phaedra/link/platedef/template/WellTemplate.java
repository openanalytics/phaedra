package eu.openanalytics.phaedra.link.platedef.template;

import java.util.HashMap;
import java.util.Map;

public class WellTemplate implements Cloneable {
	
	private int nr;
	private String wellType;
	private String compoundType;
	private String compoundNumber;
	private String concentration;
	private String remark;
	private boolean skip;
	
	private Map<String,String> annotations;
	
	private PlateTemplate plate;
	
	public WellTemplate() {
		annotations = new HashMap<>();
	}
	
	/*
	 * Getters & setters
	 * *****************
	 */
	
	public int getNr() {
		return nr;
	}
	public void setNr(int nr) {
		this.nr = nr;
	}
	public String getWellType() {
		return wellType;
	}
	public void setWellType(String wellType) {
		this.wellType = wellType;
	}
	public String getCompoundType() {
		return compoundType;
	}
	public void setCompoundType(String compoundType) {
		this.compoundType = compoundType;
	}
	public String getCompoundNumber() {
		return compoundNumber;
	}
	public void setCompoundNumber(String compoundNumber) {
		this.compoundNumber = compoundNumber;
	}
	public String getConcentration() {
		return concentration;
	}
	public void setConcentration(String concentration) {
		this.concentration = concentration;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	public PlateTemplate getPlate() {
		return plate;
	}
	public void setPlate(PlateTemplate plate) {
		this.plate = plate;
	}
	public boolean isSkip() {
		return skip;
	}
	public void setSkip(boolean skip) {
		this.skip = skip;
	}
	public Map<String, String> getAnnotations() {
		return annotations;
	}
	public void setAnnotations(Map<String, String> annotations) {
		this.annotations = annotations;
	}
	
	/*
	 * Equality is based on the nr field: it must be unique
	 * within the parent plate template.
	 */
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + nr;
		result = prime * result + ((plate == null) ? 0 : plate.hashCode());
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
		WellTemplate other = (WellTemplate) obj;
		if (nr != other.nr)
			return false;
		if (plate == null) {
			if (other.plate != null)
				return false;
		} else if (!plate.equals(other.plate))
			return false;
		return true;
	}
	
	@Override
	protected WellTemplate clone() throws CloneNotSupportedException {
		WellTemplate clone = (WellTemplate)super.clone();
		clone.setAnnotations(new HashMap<>());
		for (String ann: annotations.keySet()) clone.getAnnotations().put(ann, annotations.get(ann));
		return clone;
	}
}
