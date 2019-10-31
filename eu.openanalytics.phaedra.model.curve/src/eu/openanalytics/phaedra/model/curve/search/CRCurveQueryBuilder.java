package eu.openanalytics.phaedra.model.curve.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilder;
import eu.openanalytics.phaedra.model.curve.vo.CRCurve;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CRCurveQueryBuilder extends AbstractQueryBuilder<CRCurve> {

	public CRCurveQueryBuilder(EntityManager entityManager) {
		super(entityManager);
	}
	
	@Override
	public Class<CRCurve> getType() {
		return CRCurve.class;
	}

	@Override
	public Set<Class<? extends PlatformObject>> getDirectParentClasses() {
		return new HashSet<Class<? extends PlatformObject>>(Arrays.asList(Compound.class, Feature.class));
	}

	@Override
	public String getJoinProperty() {
		return "unused";
	}
}
