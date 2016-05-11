package eu.openanalytics.phaedra.ui.plate.grid;

import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.base.ui.gridviewer.provider.AbstractGridContentProvider;
import eu.openanalytics.phaedra.model.plate.util.PlateWellAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PlateContentProvider extends AbstractGridContentProvider {

	private Plate plate;
	private PlateWellAccessor wellAccessor;
	
	@Override
	public int getColumns(Object inputElement) {
		if (plate != null) return plate.getColumns();
		return 12;
	}

	@Override
	public int getRows(Object inputElement) {
		if (plate != null) return plate.getRows();
		return 8;
	}

	@Override
	public Object getElement(int row, int column) {
		if (wellAccessor != null) return wellAccessor.getWell(row+1, column+1);
		return null;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof Plate) {
			plate = (Plate)newInput;
			wellAccessor = new PlateWellAccessor(plate);
		}
	}
}
