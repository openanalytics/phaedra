package eu.openanalytics.phaedra.ui.plate.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellUtils {

	/**
	 * Get the wells from a selection.
	 *
	 * Works for Well, Compound, Plate and Experiment selections.
	 *
	 * @param selection
	 * @return
	 */
	public static List<Well> getWells(ISelection selection) {
		if (SelectionUtils.getFirstObject(selection, Well.class) != null) {
			return SelectionUtils.getObjects(selection, Well.class);
		}

		if (SelectionUtils.getFirstObject(selection, Compound.class) != null) {
			List<Well> wells = new ArrayList<>();
			List<Compound> compounds = SelectionUtils.getObjects(selection, Compound.class);
			for (Compound c : compounds) {
				wells.addAll(c.getWells());
			}
			return wells;
		}

		if (SelectionUtils.getFirstObject(selection, Plate.class) != null) {
			List<Well> wells = new ArrayList<>();
			List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
			for (Plate plate : plates) {
				wells.addAll(plate.getWells());
			}
			Collections.sort(wells, PlateUtils.WELL_EXP_NAME_PLATE_BARCODE_WELL_NR_SORTER);
			return wells;
		}

		if (SelectionUtils.getFirstObject(selection, Experiment.class) != null) {
			List<Well> wells = new ArrayList<>();
			List<Experiment> exps = SelectionUtils.getObjects(selection, Experiment.class);
			for (Experiment exp : exps) {
				List<Plate> plates = PlateService.getInstance().getPlates(exp);
				for (Plate plate : plates) {
					wells.addAll(plate.getWells());
				}
			}
			return wells;
		}

		return new ArrayList<>();
	}

}
