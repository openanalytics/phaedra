package eu.openanalytics.phaedra.model.protocol.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
	public <T extends PlatformObject> IQueryBuilder<T> getBuilder(Class<T> clazz) {
		if (clazz.equals(ProtocolClass.class)) {
			return (IQueryBuilder<T>) new ProtocolClassQueryBuilder();
		}
		if (clazz.equals(Protocol.class)) {
			return (IQueryBuilder<T>) new ProtocolQueryBuilder();
		}
		if (clazz.equals(Feature.class)) {
			return (IQueryBuilder<T>) new FeatureQueryBuilder();
		}

		return null;
	}
}
