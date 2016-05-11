package eu.openanalytics.phaedra.model.subwell;

import java.io.Serializable;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellItem extends PlatformObject implements Serializable {

	private static final long serialVersionUID = 8392531640309577961L;

	private Well well;
	private int index;

	public SubWellItem() {
		// Default constructor.
	}

	public SubWellItem(Well well, int index) {
		this.well = well;
		this.index = index;
	}

	public Well getWell() {
		return well;
	}

	public void setWell(Well well) {
		this.well = well;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public String toString() {
		return index + " @ " + well.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((well == null) ? 0 : well.hashCode());
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
		SubWellItem other = (SubWellItem) obj;
		if (index != other.index)
			return false;
		if (well == null && other.well != null)
			return false;
		else if (!well.equals(other.well))
			return false;
		return true;
	}

}