package eu.openanalytics.phaedra.calculation;

import java.util.List;

import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

/**
 * A convenience class for easy access to a (well or subwell) feature's classification.
 */
public class ClassificationProvider {

	private Feature wellFeature;
	private SubWellFeature subWellFeature;
	
	public ClassificationProvider(Feature wellFeature) {
		this.wellFeature = wellFeature;
	}
	
	public ClassificationProvider(SubWellFeature subWellFeature) {
		this.subWellFeature = subWellFeature;
	}
	
	public List<FeatureClass> getFeatureClasses() {
		if (wellFeature == null) return subWellFeature.getFeatureClasses();
		return wellFeature.getFeatureClasses();
	}
	
	public String getName() {
		if (wellFeature == null) return subWellFeature.getDisplayName();
		return wellFeature.getDisplayName();
	}
	
	private String getId() {
		if (wellFeature == null) return "SW" + subWellFeature.getId();
		return "W" + wellFeature.getId();
	}
	
	@Override
	public int hashCode() {
		return getId().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ClassificationProvider other = (ClassificationProvider) obj;
		String id = getId();
		String otherId = other.getId();
		return id.equals(otherId);
	}
}
