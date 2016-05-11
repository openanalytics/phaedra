package eu.openanalytics.phaedra.silo.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class ColumnDescriptor {

	public String name;
	public IFeature feature;
	public String normalization;

	private final static Pattern FEATURE_NAMING_PATTERN = Pattern.compile("(.*) \\[(.*)\\]");
	
	public static ColumnDescriptor createDescriptor(String columnName, Silo silo) {
		ColumnDescriptor descriptor = new ColumnDescriptor();
		descriptor.name = columnName;
		
		String featureName = columnName;
		Matcher matcher = FEATURE_NAMING_PATTERN.matcher(columnName);
		if (matcher.matches()) {
			featureName = matcher.group(1);
			descriptor.normalization = matcher.group(2);
		}
		
		if (silo.getType() == GroupType.WELL.getType()) {
			descriptor.feature = ProtocolUtils.getFeatureByName(featureName, silo.getProtocolClass());
		} else if (silo.getType() == GroupType.SUBWELL.getType()) {
			descriptor.feature = ProtocolUtils.getSubWellFeatureByName(featureName, silo.getProtocolClass());
		}
		
		return descriptor;
	}
	
	public static String createColumnName(IFeature feature, String normalization) {
		String columnName = feature.getDisplayName();
		if (normalization != null && !normalization.equals(NormalizationService.NORMALIZATION_NONE)) {
			columnName += " [" + normalization + "]";
		}
		return columnName;
	}
}
