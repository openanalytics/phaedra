package eu.openanalytics.phaedra.ui.silo.tree;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;

public class SiloTreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof Silo) {
			Silo silo = (Silo) inputElement;
			return silo.getDatasets().toArray();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof SiloDataset) return ((SiloDataset) element).getSilo();
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof Silo);
	}
	
	@Override
	public Object[] getChildren(Object element) {
		return getElements(element);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Nothing to do.
	}
	
	@Override
	public void dispose() {
		// Nothing to do.
	}
}
