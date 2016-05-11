package eu.openanalytics.phaedra.model.protocol.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.search.AbstractQueryBuilder;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class FeatureQueryBuilder extends AbstractQueryBuilder<Feature> {
	@Override
	public Class<Feature> getType() {
		return Feature.class;
	}

	@Override
	public Set<Class<? extends PlatformObject>> getDirectParentClasses() {
		return new HashSet<Class<? extends PlatformObject>>(Arrays.asList(ProtocolClass.class));
	}

	@Override
	public String getJoinProperty() {
		return "feature";
	}
	
}
