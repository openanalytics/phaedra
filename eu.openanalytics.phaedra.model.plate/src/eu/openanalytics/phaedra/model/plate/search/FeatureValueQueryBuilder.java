package eu.openanalytics.phaedra.model.plate.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilder;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class FeatureValueQueryBuilder extends AbstractQueryBuilder<FeatureValue> {
	
	public FeatureValueQueryBuilder(EntityManager entityManager) {
		super(entityManager);
	}
	
	@Override
	public Class<FeatureValue> getType() {
		return FeatureValue.class;
	}

	@Override
	public Set<Class<? extends PlatformObject>> getDirectParentClasses() {
		return new HashSet<Class<? extends PlatformObject>>(Arrays.asList(Well.class, Feature.class));
	}

	@Override
	public String getJoinProperty() {
		return "featureValue";
	}
}
