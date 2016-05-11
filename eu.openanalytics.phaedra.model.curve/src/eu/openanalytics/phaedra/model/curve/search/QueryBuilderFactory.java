package eu.openanalytics.phaedra.model.curve.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilderFactory;
import eu.openanalytics.phaedra.base.search.IQueryBuilder;
import eu.openanalytics.phaedra.model.curve.vo.CRCurve;

public class QueryBuilderFactory extends AbstractQueryBuilderFactory {
	@Override
	public Set<Class<? extends PlatformObject>> getSupportedTypes() {
		return new HashSet<Class<? extends PlatformObject>>(Arrays.asList(CRCurve.class));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends PlatformObject> IQueryBuilder<T> getBuilder(Class<T> clazz) {
		if (clazz.equals(CRCurve.class)) {
			return (IQueryBuilder<T>) new CRCurveQueryBuilder();
		}
		return null;
	}
}
