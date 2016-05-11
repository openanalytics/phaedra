package eu.openanalytics.phaedra.model.plate.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

/**
 * Convenience class for easy row- and column-based
 * access to a plate's wells.
 */
public class PlateWellAccessor {

	private Well[][] grid;
	private Plate plate;
	
	public PlateWellAccessor(Plate plate) {
		this.plate = plate;
		
		grid = new Well[plate.getRows()][plate.getColumns()];
		for (Well well: plate.getWells()) {
			grid[well.getRow()-1][well.getColumn()-1] = well;
		}
	}
	
	public Well getWell(int row, int column) {
		return grid[row-1][column-1];
	}
	
	public Plate getPlate() {
		return plate;
	}
	
	public List<Well> getWells() {
		List<Well> wells = new ArrayList<Well>(plate.getWells());
		Collections.sort(wells, PlateUtils.WELL_NR_SORTER);
		return wells;
	}
}
