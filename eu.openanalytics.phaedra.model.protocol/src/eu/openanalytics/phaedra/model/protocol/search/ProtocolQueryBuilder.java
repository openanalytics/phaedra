package eu.openanalytics.phaedra.model.protocol.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilder;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ProtocolQueryBuilder extends AbstractQueryBuilder<Protocol> {
	
	public ProtocolQueryBuilder(EntityManager entityManager) {
		super(entityManager);
	}
	
	@Override
	public Class<Protocol> getType() {
		return Protocol.class;
	}
	
	@Override
	public Set<Class<? extends PlatformObject>> getDirectParentClasses() {
		return new HashSet<Class<? extends PlatformObject>>(Arrays.asList(ProtocolClass.class));
	} 
	
	@Override
	public String getJoinProperty() {
		return "protocol";
	}
}
