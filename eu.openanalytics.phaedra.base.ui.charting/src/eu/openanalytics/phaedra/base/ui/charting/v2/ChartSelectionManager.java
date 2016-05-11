package eu.openanalytics.phaedra.base.ui.charting.v2;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class ChartSelectionManager implements ISelectionProvider {

	private ListenerList listeners;
	private ISelection selection;

	public ChartSelectionManager() {
		listeners = new ListenerList();
	}

	public void fireSelection() {
		SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
		for (Object listener : listeners.getListeners()) {
			((ISelectionChangedListener) listener).selectionChanged(event);
		}
	}

	@Override
	public ISelection getSelection() {
		return selection;
	}

	@Override
	public void setSelection(ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);
	}
}