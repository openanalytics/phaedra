package eu.openanalytics.phaedra.model.protocol.vo;

import java.util.List;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedObject;

/**
 * General interface for features: well and subwell features.
 */
public interface IFeature extends IValueObject, IOwnedObject {

	public String getName();

	public String getShortName();
	
	public String getDisplayName();
	
	public String getDescription();

	public String getFormatString();

	public ProtocolClass getProtocolClass();
	
	public List<FeatureClass> getFeatureClasses();
	
	public boolean isNumeric();

	public boolean isKey();
	
	public FeatureGroup getFeatureGroup();
}
