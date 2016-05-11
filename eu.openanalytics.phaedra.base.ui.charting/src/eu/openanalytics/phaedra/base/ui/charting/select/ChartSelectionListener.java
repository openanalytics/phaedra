package eu.openanalytics.phaedra.base.ui.charting.select;

import java.util.Collection;

@FunctionalInterface
public interface ChartSelectionListener<E> {

	void selectionChanged(Collection<E> selectedEntities);

}
