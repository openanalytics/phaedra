package eu.openanalytics.phaedra.model.plate.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilder;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class WellQueryBuilder extends AbstractQueryBuilder<Well> {
	
	public WellQueryBuilder(EntityManager entityManager) {
		super(entityManager);
	}
	
	@Override
	public Class<Well> getType() {
		return Well.class;
	}

	@Override
	public Set<Class<? extends PlatformObject>> getDirectParentClasses() {
		return new HashSet<Class<? extends PlatformObject>>(Arrays.asList(Plate.class, Compound.class));
	}

	@Override
	public String getJoinProperty() {
		return "well";
	}
}
