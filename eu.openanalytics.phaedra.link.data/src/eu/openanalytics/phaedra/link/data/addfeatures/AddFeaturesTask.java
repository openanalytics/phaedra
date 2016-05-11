package eu.openanalytics.phaedra.link.data.addfeatures;

import java.util.List;

import eu.openanalytics.phaedra.datacapture.util.FeatureDefinition;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class AddFeaturesTask {

	public ProtocolClass targetProtocolClass;
	public Class<? extends IFeature> featureClass;
	public List<FeatureDefinition> featureDefinitions;
	
}

