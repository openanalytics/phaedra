package eu.openanalytics.phaedra.model.protocol.search;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilder;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ProtocolClassQueryBuilder extends AbstractQueryBuilder<ProtocolClass> {
	
	public ProtocolClassQueryBuilder(EntityManager entityManager) {
		super(entityManager);
	}
	
	@Override
	public Class<ProtocolClass> getType() {
		return ProtocolClass.class;
	}
		
	@Override
	public Set<Class<? extends PlatformObject>> getDirectParentClasses() {
		return new HashSet<>();
	}
	
	@Override
	public String getJoinProperty() {
		return "protocolClass";
	}	
}
