package eu.openanalytics.phaedra.ui.plate.util;

import java.util.List;

import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PlateContentProvider extends ExperimentContentProvider {
	@Override
	public Object getParent(Object child) {
		if (child instanceof Plate) {
			return ((Plate) child).getExperiment();
		}
		
		return super.getParent(child);
	}
	
	@Override
	public boolean hasChildren(Object parent) {
		if (parent instanceof Experiment) {
			Experiment experiment = (Experiment) parent;
			List<Plate> plates = PlateService.getInstance().getPlates(experiment);
			return plates != null && !plates.isEmpty();
		}
		
		return super.hasChildren(parent);
	}
	
	@Override
	public Object[] getChildren(Object parent) {		
		if (parent instanceof Experiment) {
			Experiment experiment = (Experiment) parent;
			List<Plate> plates = PlateService.getInstance().getPlates(experiment);
			return plates.toArray();
		}
		
		return super.getChildren(parent);
	}
	
	@Override
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}	
}
