package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.base.ui.gridviewer.provider.AbstractGridContentProvider;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class FeatureContentProvider<FEATURE extends IFeature> extends AbstractGridContentProvider {

	private BaseFeatureInput<FEATURE> input;

	@Override
	public int getColumns(Object inputElement) {
		if (input == null || input.getSelectedFeatures() == null || input.getSelectedFeatures().isEmpty()) return 3;
		return input.getSelectedFeatures().size();
	}

	@Override
	public int getRows(Object inputElement) {
		if (input == null || input.getSelectedFeatures() == null || input.getSelectedFeatures().isEmpty()) return 3;
		return input.getSelectedFeatures().size();
	}

	@Override
	public Object getElement(int row, int column) {
		List<FEATURE> selectedFeatures = new ArrayList<>();
		if (input != null && input.getSelectedFeatures() != null && input.getSelectedFeatures().size() > Math.max(row, column)) {
			// We only want the Features that belong to the current Cell.
			selectedFeatures.add(input.getSelectedFeatures().get(column));
			selectedFeatures.add(input.getSelectedFeatures().get(row));
		}
		return selectedFeatures;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof BaseFeatureInput) {
			input = (BaseFeatureInput<FEATURE>) newInput;
		}
	}

}