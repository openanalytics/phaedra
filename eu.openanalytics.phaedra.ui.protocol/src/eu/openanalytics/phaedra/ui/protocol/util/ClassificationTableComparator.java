package eu.openanalytics.phaedra.ui.protocol.util;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;

import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;

public class ClassificationTableComparator extends ViewerComparator {
	
	private static final int DESCENDING = 1;
	
	private int propertyIndex;
	private int direction;

	public ClassificationTableComparator() {
		this.propertyIndex = 0;
		this.direction = 0;
	}

	public int getDirection() {
		return direction == 1 ? SWT.DOWN : SWT.UP;
	}

	public void setColumn(int column) {
		if (column == this.propertyIndex) {
			// Same column as last sort; toggle the direction
			direction = 1 - direction;
		} else {
			// New column; do an ascending sort
			this.propertyIndex = column;
			direction = 0;
		}
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		FeatureClass fc1 = (FeatureClass) e1;
		FeatureClass fc2 = (FeatureClass) e2;
		int rc = 0;
		switch (propertyIndex) {
		case 0:
			rc = fc1.getPattern().compareTo(fc2.getPattern());
			break;
		case 1:
			rc = fc1.getPatternType().compareTo(fc2.getPatternType());
			break;
		case 2:
			rc = fc1.getRgbColor() - fc2.getRgbColor();
			break;
		case 3:
			rc = fc1.getLabel().compareTo(fc2.getLabel());
			break;
		case 4:
			rc = fc1.getSymbol().compareTo(fc2.getSymbol());
			break;
//		case 5:
//			rc = fc1.getDescription().compareTo(fc2.getDescription());
//			break;
		default:
			rc = 0;
		}

		// If descending order, flip the direction
		if (direction == DESCENDING) {
			rc = -rc;
		}
		
		return rc;
	}

} 