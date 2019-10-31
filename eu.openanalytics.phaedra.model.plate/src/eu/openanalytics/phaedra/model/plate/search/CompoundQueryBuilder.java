package eu.openanalytics.phaedra.model.plate.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilder;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class CompoundQueryBuilder extends AbstractQueryBuilder<Compound> {
	
	public CompoundQueryBuilder(EntityManager entityManager) {
		super(entityManager);
	}
	
	@Override
	public Class<Compound> getType() {
		return Compound.class;
	}

	@Override
	public Set<Class<? extends PlatformObject>> getDirectParentClasses() {
		return new HashSet<Class<? extends PlatformObject>>(Arrays.asList(Plate.class));
	}

	@Override
	public String getJoinProperty() {
		return "compound";
	}
}
