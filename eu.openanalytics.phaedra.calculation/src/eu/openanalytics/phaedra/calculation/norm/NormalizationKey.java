package eu.openanalytics.phaedra.calculation.norm;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class NormalizationKey {

	private IValueObject dataToNormalize;
	private IFeature feature;
	
	private String normalization;
	
	public NormalizationKey() {
		// Default constructor
	}
	
	public NormalizationKey(Plate plate, Feature feature, String normalization) {
		this.dataToNormalize = plate;
		this.feature = feature;
		this.normalization = normalization;
	}
	
	public NormalizationKey(Well well, SubWellFeature feature, String normalization) {
		this.dataToNormalize = well;
		this.feature = feature;
		this.normalization = normalization;
	}
	
	public IValueObject getDataToNormalize() {
		return dataToNormalize;
	}
	
	public IFeature getFeature() {
		return feature;
	}
	
	public String getNormalization() {
		return normalization;
	}
}
