package eu.openanalytics.phaedra.ui.silo.tree;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.silo.SiloDataService;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.util.SiloStructure;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SiloTreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof Silo) {
			try {
				Silo silo = (Silo)inputElement;
				SiloStructure structure = SiloDataService.getInstance().getSiloStructure(silo);
				return getChildren(structure);
			} catch (SiloException e) {
				throw new RuntimeException("Failed to retrieve silo structure", e);
			}
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return ((SiloStructure)element).getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		return !((SiloStructure)element).getChildren().isEmpty(); 
	}
	
	@Override
	public Object[] getChildren(Object element) {
		return ((SiloStructure)element).getChildren().toArray();
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
