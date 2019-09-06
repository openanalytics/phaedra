package eu.openanalytics.phaedra.datacapture.util;

import java.util.List;

import eu.openanalytics.phaedra.datacapture.Activator;

public interface IMissingFeaturesHandler {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".missingFeaturesHandler";
	public final static String ATTR_CLASS = "class";

	public boolean handle(List<FeatureDefinition> missingFeatures, MissingFeaturesHelper helper);

}
