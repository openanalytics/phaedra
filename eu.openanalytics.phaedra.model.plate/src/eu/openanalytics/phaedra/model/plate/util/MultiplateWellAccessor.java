package eu.openanalytics.phaedra.model.plate.util;

/**
 * Convenience class for access to the wells of a set of plates.
 */

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class MultiplateWellAccessor {

	private List<Well> wellList;     //plain list of all wells from all plates
	private List<Plate> plates;       
	
	public MultiplateWellAccessor(List<Plate> plates) {
		this.plates = plates;
		
		wellList = new ArrayList<Well>();
		for(Plate plate : plates)
		{
			for (Well well: plate.getWells()) {
				wellList.add(well);
			}
		}
	}
	
//	//TODO: how to identify the plate, by plate-ID or by index into 'plates'???
//	@SuppressWarnings("deprecation")
//	public Well getWell(int row, int column) throws NotHandledException {
//		throw new NotHandledException("TODO: how to identify the plate, by plate-ID or by index into 'plates'???");
//		//return grid[row-1][column-1];
//	}
	
	public List<Plate> getPlates() {
		return plates;
	}
	
	public List<Well> getWells() {
		return wellList;
	}
}
