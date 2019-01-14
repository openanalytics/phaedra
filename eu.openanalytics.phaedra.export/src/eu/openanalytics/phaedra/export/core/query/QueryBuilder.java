package eu.openanalytics.phaedra.export.core.query;

import java.util.Date;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.ExportSettings.Includes;
import eu.openanalytics.phaedra.export.core.filter.LibraryFilter;
import eu.openanalytics.phaedra.export.core.filter.QualifierFilter;
import eu.openanalytics.phaedra.export.core.filter.WellFeatureFilter;
import eu.openanalytics.phaedra.export.core.util.SQLUtils;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveFitSettings;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.ICurveFitModel;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Definition;
import eu.openanalytics.phaedra.model.curve.CurveParameter.ParameterType;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class QueryBuilder {

	public Query createBaseQuery(ExportSettings settings) {
		StringBuilder sb = new StringBuilder();

		sb.append("SELECT");
		sb.append(" P.EXPERIMENT_ID, E.EXPERIMENT_NAME, P.PLATE_ID, P.BARCODE, P.SEQUENCE_IN_RUN, P.PLATE_INFO, P.VALIDATE_STATUS, P.APPROVE_STATUS,");
		sb.append(" " + JDBCUtils.selectConcat("P.DESCRIPTION", "W.DESCRIPTION", ' ') + " REMARKS, W.WELL_ID, W.ROW_NR, W.COL_NR, W.WELLTYPE_CODE,");
		if (settings.compoundNameSplit) sb.append(" PC.COMPOUND_TY, PC.COMPOUND_NR,");
		else sb.append(" PC.COMPOUND_TY || PC.COMPOUND_NR COMP_NAME,");
		sb.append(" W.CONCENTRATION, W.IS_VALID");
		if (settings.includes.contains(Includes.Saltform)) sb.append(", PC.SALTFORM");

		sb.append(" FROM");
		sb.append(" PHAEDRA.HCA_PLATE P, PHAEDRA.HCA_EXPERIMENT E, PHAEDRA.HCA_PLATE_WELL W");
		sb.append(" LEFT OUTER JOIN PHAEDRA.HCA_PLATE_COMPOUND PC ON PC.PLATECOMPOUND_ID = W.PLATECOMPOUND_ID");
		
		sb.append(" WHERE");
		sb.append(" E.EXPERIMENT_ID = P.EXPERIMENT_ID AND W.PLATE_ID = P.PLATE_ID");
		sb.append(" AND E.EXPERIMENT_ID IN (${experimentIds})");

		addPlateFilters(sb, settings);
		addWellFilters(sb, settings);

		sb.append(" ORDER BY P.PLATE_ID, W.ROW_NR, W.COL_NR");

		String sql = sb.toString();

		// Insert the experiment IDs.
		String expIds = "";
		for (Experiment exp: settings.experiments) {
			expIds += exp.getId()+",";
		}
		expIds = expIds.substring(0,expIds.lastIndexOf(','));
		sql = sql.replace("${experimentIds}", expIds);

		Query query = new Query();
		query.setSql(sql);
		return query;
	}

	public Query createFeatureQuery(Feature feature, ExportSettings settings) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT");

		sb.append(" F.FEATURE_NAME, F.SHORT_NAME as FEATURE_ALIAS,");
		String norm = feature.getNormalization();
		if (norm != null && !norm.equals(NormalizationService.NORMALIZATION_NONE)) {
			appendIfIncludes(Includes.NormalizedValue, settings, sb, " FV.NORMALIZED_VALUE as NORMALIZED,", null, null);
		}

		String rawVal = (feature.isNumeric()) ? " FV.RAW_NUMERIC_VALUE as RAW_VALUE," : " FV.RAW_STRING_VALUE as RAW_VALUE,";
		appendIfIncludes(Includes.RawValue, settings, sb, rawVal, null, null);

		if (settings.includes.contains(Includes.CurveProperties) || settings.includes.contains(Includes.CurvePropertiesAll)) {
			Includes inc = settings.includes.contains(Includes.CurveProperties) ? Includes.CurveProperties : Includes.CurvePropertiesAll;
			
			CurveFitSettings fitSettings = CurveFitService.getInstance().getSettings(feature);
			if (fitSettings != null) {
				ICurveFitModel model = CurveFitService.getInstance().getModel(fitSettings.getModelId());
				
				String baseCurveQuery = " (SELECT c.${propertyName} FROM PHAEDRA.HCA_CURVE C WHERE C.CURVE_ID = WC.CURVE_ID) AS ${columnAlias},";
				appendIfIncludes(Includes.CurveProperties, settings, sb, baseCurveQuery, "MODEL_ID", "MODEL");
				
				for (Definition def: model.getOutputParameters()) {
					if (!def.key && !settings.includes.contains(Includes.CurvePropertiesAll)) continue;
					if (def.type == ParameterType.Binary) continue;
					
					String basePropertyQuery = " (SELECT CP.NUMERIC_VALUE"
							+ " FROM PHAEDRA.HCA_CURVE_PROPERTY CP WHERE CP.CURVE_ID = WC.CURVE_ID AND CP.PROPERTY_NAME = '${propertyName}') AS ${columnAlias},";
					String baseCensoredPropertyQuery ="(SELECT CP.STRING_VALUE"
							+ " FROM PHAEDRA.HCA_CURVE_PROPERTY CP WHERE CP.CURVE_ID = WC.CURVE_ID AND CP.PROPERTY_NAME = '${propertyName} Censor')"
							+ " || (SELECT " + JDBCUtils.getFormatNumberSQL("CP.NUMERIC_VALUE", 3)
							+ " FROM PHAEDRA.HCA_CURVE_PROPERTY CP WHERE CP.CURVE_ID = WC.CURVE_ID AND CP.PROPERTY_NAME = '${propertyName}') AS ${columnAlias},";
					appendIfIncludes(inc, settings, sb, (CurveParameter.isCensored(def) ? baseCensoredPropertyQuery : basePropertyQuery), def.name, def.name);
				}
			}
		}

		if (sb.charAt(sb.length()-1) == ',') sb.deleteCharAt(sb.length()-1);

		sb.append(" FROM");
		sb.append(" PHAEDRA.HCA_FEATURE F, PHAEDRA.HCA_PLATE P, PHAEDRA.HCA_PLATE_WELL W");
		sb.append(" LEFT OUTER JOIN PHAEDRA.HCA_FEATURE_VALUE FV ON FV.WELL_ID = W.WELL_ID AND FV.FEATURE_ID = ${featureId}");
		sb.append(" LEFT OUTER JOIN PHAEDRA.HCA_PLATE_COMPOUND PC ON PC.PLATECOMPOUND_ID = W.PLATECOMPOUND_ID");
		sb.append(" LEFT OUTER JOIN PHAEDRA.HCA_WELL_CURVES WC ON WC.WELL_ID = W.WELL_ID AND WC.FEATURE_ID = ${featureId}");

		sb.append(" WHERE");
		sb.append(" W.PLATE_ID = P.PLATE_ID AND F.FEATURE_ID = ${featureId}");
		sb.append(" AND P.EXPERIMENT_ID IN (${experimentIds})");

		addPlateFilters(sb, settings);
		addWellFilters(sb, settings);

		sb.append(" ORDER BY P.PLATE_ID, W.ROW_NR, W.COL_NR");

		String sql = sb.toString();

		// Insert the feature ID.
		sql = sql.replace("${featureId}", ""+feature.getId());

		// Insert the experiment IDs.
		String expIds = "";
		for (Experiment exp: settings.experiments) {
			expIds += exp.getId()+",";
		}
		expIds = expIds.substring(0,expIds.lastIndexOf(','));
		sql = sql.replace("${experimentIds}", expIds);

		Query query = new Query();
		query.setSql(sql);
		return query;
	}

	private void appendIfIncludes(Includes inc, ExportSettings settings, StringBuilder sb, String baseString, String propName, String colAlias) {
		if (settings.includes.contains(inc)) {
			String string = baseString;
			if (propName != null) string = string.replace("${propertyName}", propName);
			if (colAlias != null) string = string.replace("${columnAlias}", colAlias.replace(" ", "_"));
			sb.append(string);
		}
	}

	private void addPlateFilters(StringBuilder sb, ExportSettings settings) {

		if (settings.library != null && !settings.library.equals(LibraryFilter.ALL)) {
			sb.append(" AND EXTRACTVALUE(P.DATA_XML,'/data/properties/property[@key=\"plate-library\"]/@value') = '" + settings.library + "'");
		}

		if (settings.plateQualifier != null && !settings.plateQualifier.equals(QualifierFilter.ALL)) {
			sb.append(" AND EXTRACTVALUE(P.DATA_XML,'/data/properties/property[@key=\"plate-qualifier\"]/@value') = '" + settings.plateQualifier + "'");
		}

		if (settings.filterValidation) {
			Date from = settings.validationDateFrom;
			Date to = settings.validationDateTo;
			if (to != null) {
				// Add 1 day to the end date, so that the end date itself is included in the range.
				long oneDay = 86400000L;
				to.setTime(to.getTime() + oneDay);
			}
			if (from != null && to != null) {
				sb.append(" AND P.VALIDATE_DT BETWEEN ");
				sb.append(SQLUtils.generateSQLDate(from));
				sb.append(" AND ");
				sb.append(SQLUtils.generateSQLDate(to));
			} else if (from != null) {
				sb.append(" AND P.VALIDATE_DT > ");
				sb.append(SQLUtils.generateSQLDate(from));
			} else if (to != null) {
				sb.append(" AND P.VALIDATE_DT < ");
				sb.append(SQLUtils.generateSQLDate(to));
			}

			String user = settings.validationUser;
			if (user != null && !user.isEmpty()) {
				sb.append(" AND P.VALIDATE_USER LIKE '%" + user + "%'");
			}
		}

		if (settings.filterApproval) {
			Date from = settings.approvalDateFrom;
			Date to = settings.approvalDateTo;
			if (to != null) {
				// Add 1 day to the end date, so that the end date itself is included in the range.
				long oneDay = 86400000L;
				to.setTime(to.getTime() + oneDay);
			}
			if (from != null && to != null) {
				sb.append(" AND P.APPROVE_DT BETWEEN ");
				sb.append(SQLUtils.generateSQLDate(from));
				sb.append(" AND ");
				sb.append(SQLUtils.generateSQLDate(to));
			} else if (from != null) {
				sb.append(" AND P.APPROVE_DT > ");
				sb.append(SQLUtils.generateSQLDate(from));
			} else if (to != null) {
				sb.append(" AND P.APPROVE_DT < ");
				sb.append(SQLUtils.generateSQLDate(to));
			}

			String user = settings.approvalUser;
			if (user != null && !user.isEmpty()) {
				sb.append(" AND P.APPROVE_USER LIKE '%" + user + "%'");
			}
		}
	}

	private void addWellFilters(StringBuilder sb, ExportSettings settings) {

		if (!settings.includeRejectedWells) {
			sb.append(" AND W.IS_VALID >= 0");
		}

		if (!settings.includeInvalidatedCompounds) {
			sb.append(" AND (PC.VALIDATE_STATUS IS NULL OR PC.VALIDATE_STATUS >= 0)");
		}

		if (!settings.includeInvalidatedPlates) {
			sb.append(" AND (P.VALIDATE_STATUS IS NULL OR P.VALIDATE_STATUS >= 0)");
		}

		if (!settings.includeDisapprovedPlates) {
			sb.append(" AND (P.APPROVE_STATUS IS NULL OR P.APPROVE_STATUS >= 0)");
		}

		if (settings.filterWellResults) {
			sb.append(" AND EXISTS (SELECT * FROM PHAEDRA.HCA_FEATURE_VALUE");
			sb.append(" WHERE FEATURE_ID = " + settings.wellResultFeature.getId());
			sb.append(" AND WELL_ID = W.WELL_ID");
			if (settings.wellResultNormalization.equals(WellFeatureFilter.NORM_NONE)) {
				sb.append(" AND RAW_NUMERIC_VALUE ");
			} else {
				sb.append(" AND NORMALIZED_VALUE ");
			}
			sb.append(settings.wellResultOperator + " " + settings.wellResultValue + ")");
		}

		if (settings.filterCompound) {
			sb.append(" AND (");
			for (int i=0; i<settings.compoundNumbers.length; i++) {
				String type = settings.compoundTypes[i];
				String compound = settings.compoundNumbers[i];
				if (i > 0) sb.append(" OR ");
				sb.append("(PC.COMPOUND_TY = '" + type + "' AND PC.COMPOUND_NR = '" + compound + "')");
			}
			sb.append(")");
		}

		if (settings.wellTypes != null) {
			String wellTypes = StringUtils.createSeparatedString(settings.wellTypes, "','", false);
			sb.append(" AND W.WELLTYPE_CODE IN ('" + wellTypes + "')");
		}
	}
}
