package eu.openanalytics.phaedra.model.subwell;

import java.util.BitSet;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.model.plate.vo.Well;

/**
 * Represents a group of multiple subwell items in a single well.
 * This object is used for better performance of subwell items in JFace selections.
 */
public class SubWellSelection extends PlatformObject {

	private Well well;

	private BitSet indices;

	public SubWellSelection(Well well, BitSet indices) {
		this.well = well;
		this.indices = indices;
	}

	public Well getWell() {
		return well;
	}

	public BitSet getIndices() {
		return indices;
	}

	@Override
	public String toString() {
		return (well == null ? "Well null" : well.toString()) + " " + (indices == null ? "null" : indices.toString());
	}

}