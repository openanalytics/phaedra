package eu.openanalytics.phaedra.base.ui.nattable.selection;

import java.util.List;

import org.eclipse.jface.viewers.ISelection;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;

public class SelectionTransformer<T> implements ISelectionTransformer<T> {

	private Class<T> type;

	protected SelectionTransformer() {
		// Do nothing.
	}

	public SelectionTransformer(Class<T> type) {
		this.type = type;
	}

	@Override
	public List<?> transformOutgoingSelection(List<T> list) {
		return list;
	}

	@Override
	public List<T> transformIngoingSelection(ISelection selection) {
		return SelectionUtils.getObjects(selection, type);
	}

}