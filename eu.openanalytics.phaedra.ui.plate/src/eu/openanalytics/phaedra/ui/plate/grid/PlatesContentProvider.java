package eu.openanalytics.phaedra.ui.plate.grid;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.base.ui.gridviewer.provider.AbstractGridContentProvider;
import eu.openanalytics.phaedra.model.plate.util.PlateWellAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class PlatesContentProvider extends AbstractGridContentProvider {

	private List<Plate> plates;
	private List<PlateWellAccessor> wellAccessors;

	@Override
	public int getColumns(Object inputElement) {
		if (plates != null && !plates.isEmpty()) return plates.get(0).getColumns();
		return 12;
	}

	@Override
	public int getRows(Object inputElement) {
		if (plates != null && !plates.isEmpty()) return plates.get(0).getRows();
		return 8;
	}

	@Override
	public Object getElement(int row, int column) {
		if (wellAccessors != null && !wellAccessors.isEmpty()) {
			List<Well> wells = new ArrayList<>();
			for (PlateWellAccessor wellAccessor : wellAccessors)
				wells.add(wellAccessor.getWell(row+1, column+1));
			return wells;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof List) {
//			if (oldInput.equals(newInput)) return;
			plates = (List<Plate>)newInput;
			wellAccessors = new ArrayList<>();
			for (Plate plate : plates)
				wellAccessors.add(new PlateWellAccessor(plate));
		}
	}
}
