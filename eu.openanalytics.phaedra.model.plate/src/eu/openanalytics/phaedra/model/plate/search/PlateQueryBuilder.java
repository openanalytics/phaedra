package eu.openanalytics.phaedra.model.plate.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilder;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PlateQueryBuilder extends AbstractQueryBuilder<Plate> {
	
	public PlateQueryBuilder(EntityManager entityManager) {
		super(entityManager);
	}
	
	@Override
	public Class<Plate> getType() {
		return Plate.class;
	}

	@Override
	public Set<Class<? extends PlatformObject>> getDirectParentClasses() {
		return new HashSet<Class<? extends PlatformObject>>(Arrays.asList(Experiment.class));
	}

	@Override
	public String getJoinProperty() {
		return "plate";
	}

}
