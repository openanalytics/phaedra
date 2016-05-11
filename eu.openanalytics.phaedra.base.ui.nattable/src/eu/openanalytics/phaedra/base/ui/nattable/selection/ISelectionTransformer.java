package eu.openanalytics.phaedra.base.ui.nattable.selection;

import java.util.List;

import org.eclipse.jface.viewers.ISelection;

public interface ISelectionTransformer<T> {

	List<?> transformOutgoingSelection(List<T> list);

	List<T> transformIngoingSelection(ISelection selection);
	
}