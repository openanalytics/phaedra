package eu.openanalytics.phaedra.model.protocol.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilderFactory;
import eu.openanalytics.phaedra.base.search.IQueryBuilder;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class QueryBuilderFactory extends AbstractQueryBuilderFactory {
	
	@Override
	public Set<Class<? extends PlatformObject>> getSupportedTypes() {
		return new HashSet<Class<? extends PlatformObject>>(Arrays.asList(ProtocolClass.class, Protocol.class, Feature.class));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends PlatformObject> IQueryBuilder<T> getBuilder(Class<T> clazz, EntityManager entityManager) {
		if (clazz.equals(ProtocolClass.class)) {
			return (IQueryBuilder<T>) new ProtocolClassQueryBuilder(entityManager);
		}
		if (clazz.equals(Protocol.class)) {
			return (IQueryBuilder<T>) new ProtocolQueryBuilder(entityManager);
		}
		if (clazz.equals(Feature.class)) {
			return (IQueryBuilder<T>) new FeatureQueryBuilder(entityManager);
		}

		return null;
	}
}
