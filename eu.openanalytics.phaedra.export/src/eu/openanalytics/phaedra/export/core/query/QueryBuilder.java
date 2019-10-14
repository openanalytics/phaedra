package eu.openanalytics.phaedra.export.core.query;

import static eu.openanalytics.phaedra.export.core.query.Query.checkColumnLabel;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.export.core.ExportPlateTableSettings;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.IFilterPlatesSettings;
import eu.openanalytics.phaedra.export.core.filter.WellFeatureFilter;
import eu.openanalytics.phaedra.export.core.util.SQLUtils;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveFitSettings;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Definition;
import eu.openanalytics.phaedra.model.curve.ICurveFitModel;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;


public class QueryBuilder {

	public Query createWellsQuery(ExportSettings settings) {
		Query query = new Query();
		StringBuilder sb = new StringBuilder();

		sb.append("SELECT");
		sb.append(" P.EXPERIMENT_ID, E.EXPERIMENT_NAME, P.PLATE_ID, P.BARCODE, P.SEQUENCE_IN_RUN, P.PLATE_INFO, P.VALIDATE_STATUS, P.APPROVE_STATUS,");
		sb.append(" " + JDBCUtils.selectConcat("P.DESCRIPTION", "W.DESCRIPTION", ' ') + " REMARKS, W.WELL_ID, W.ROW_NR, W.COL_NR, W.WELLTYPE_CODE,");
		if (settings.getCompoundNameSplit()) sb.append(" PC.COMPOUND_TY, PC.COMPOUND_NR,");
		else sb.append(" PC.COMPOUND_TY || PC.COMPOUND_NR COMP_NAME,");
		sb.append(" W.CONCENTRATION, W.IS_VALID");
		query.setColumnDataType("CONCENTRATION", WellProperty.Concentration.getDataDescription());
		if (settings.includes.contains(ExportSettings.Includes.Saltform)) sb.append(", PC.SALTFORM");

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

		query.setSql(sql);
		return query;
	}

	public Query createFeatureQuery(Feature feature, ExportSettings settings) {
		Query query = new Query();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT");

		sb.append(" F.FEATURE_NAME, F.SHORT_NAME as FEATURE_ALIAS,");
		String norm = feature.getNormalization();
		if (norm != null && !norm.equals(NormalizationService.NORMALIZATION_NONE)) {
			appendIfIncludes(ExportSettings.Includes.NormalizedValue, settings, sb, " FV.NORMALIZED_VALUE as NORMALIZED,", null, null);
		}

		String rawVal = (feature.isNumeric()) ? " FV.RAW_NUMERIC_VALUE as RAW_VALUE," : " FV.RAW_STRING_VALUE as RAW_VALUE,";
		appendIfIncludes(ExportSettings.Includes.RawValue, settings, sb, rawVal, null, null);

		if (settings.includes.contains(ExportSettings.Includes.CurveProperties) || settings.includes.contains(ExportSettings.Includes.CurvePropertiesAll)) {
			ExportSettings.Includes inc = settings.includes.contains(ExportSettings.Includes.CurveProperties) ? ExportSettings.Includes.CurveProperties : ExportSettings.Includes.CurvePropertiesAll;
			
			CurveFitSettings fitSettings = CurveFitService.getInstance().getSettings(feature);
			if (fitSettings != null) {
				ICurveFitModel model = CurveFitService.getInstance().getModel(fitSettings.getModelId());
				
				String baseCurveQuery = " (SELECT c.${propertyName} FROM PHAEDRA.HCA_CURVE C WHERE C.CURVE_ID = WC.CURVE_ID) AS ${columnAlias},";
				appendIfIncludes(ExportSettings.Includes.CurveProperties, settings, sb, baseCurveQuery, "MODEL_ID", "MODEL");
				
				List<Definition> outputParamDefs = (settings.includes.contains(ExportSettings.Includes.CurvePropertiesAll)) ?
						model.getOutputParameters(fitSettings) : model.getOutputKeyParameters();
				for (Definition def: outputParamDefs) {
					final String basePropertyQuery;
					switch (def.getDataDescription().getDataType()) {
					case Real:
						basePropertyQuery = " (SELECT CP.NUMERIC_VALUE"
								+ " FROM PHAEDRA.HCA_CURVE_PROPERTY CP WHERE CP.CURVE_ID = WC.CURVE_ID AND CP.PROPERTY_NAME = '${propertyName}') AS ${columnAlias},";
						break;
					case String:
						basePropertyQuery = " (SELECT CP.STRING_VALUE"
								+ " FROM PHAEDRA.HCA_CURVE_PROPERTY CP WHERE CP.CURVE_ID = WC.CURVE_ID AND CP.PROPERTY_NAME = '${propertyName}') AS ${columnAlias},";
						break;
					default:
						continue;
					}
					final String label = checkColumnLabel(def.getName());
					query.setColumnDataType(label, def.getDataDescription());
					append(sb, basePropertyQuery, def.getName(), label);
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
		sql = sql.replace("${featureId}", Long.toString(feature.getId()));

		// Insert the experiment IDs.
		sql = sql.replace("${experimentIds}", settings.getExperiments().stream()
				.map((experiment) -> Long.toString(experiment.getId()))
				.collect(Collectors.joining(",")));

		query.setSql(sql);
		return query;
	}

	private void appendIfIncludes(ExportSettings.Includes inc, ExportSettings settings, StringBuilder sb, String baseString, String propName, String colAlias) {
		if (settings.includes.contains(inc)) {
			append(sb, baseString, propName, (colAlias != null) ? checkColumnLabel(colAlias) : null);
		}
	}
	
	private void append(StringBuilder sb, String baseString, String propName, String columnLabel) {
		String s = baseString;
		if (propName != null) s = s.replace("${propertyName}", propName);
		if (columnLabel != null) s = s.replace("${columnAlias}", columnLabel);
		sb.append(s);
	}
	
	public Query createPlatesQuery(ExportPlateTableSettings settings) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("select"
				+ " p.EXPERIMENT_ID, e.EXPERIMENT_NAME,"
				+ " p.PLATE_ID, p.SEQUENCE_IN_RUN, p.BARCODE, p.PLATE_INFO,"
				+ " p.VALIDATE_STATUS, p.APPROVE_STATUS,"
				+ " p.DESCRIPTION");
		
		if (settings.getIncludes().contains(ExportPlateTableSettings.Includes.PlateSummary)) {
			// see eu.openanalytics.phaedra.model.plate.PlateService.getPlateSummary(Plate)
			sb.append(", (select count(pc.PLATECOMPOUND_ID) from phaedra.hca_plate_compound pc"
							+ " where pc.PLATE_ID = p.PLATE_ID"
								+ " and (select count(w.WELL_ID) from phaedra.hca_plate_well w where w.PLATECOMPOUND_ID = pc.PLATECOMPOUND_ID) > 3)"
							+ " DRC_COUNT"
					+ ", (select count(pc.PLATECOMPOUND_ID) from phaedra.hca_plate_compound pc"
							+ " where pc.PLATE_ID = p.PLATE_ID"
								+ " and (select count(w.WELL_ID) from phaedra.hca_plate_well w where w.PLATECOMPOUND_ID = pc.PLATECOMPOUND_ID) <= 3)"
							+ " SDP_COUNT");
		}
		if (settings.getIncludes().contains(ExportPlateTableSettings.Includes.ApproveAndValidationDetail)) {
			sb.append(", p.VALIDATE_USER, p.VALIDATE_DT AS VALIDATE_DATE,"
					+ " p.APPROVE_USER, p.APPROVE_DT AS APPROVE_DATE");
		}
		
		sb.append(" from");
		sb.append(" phaedra.HCA_PLATE p, phaedra.HCA_EXPERIMENT e");
		
		sb.append(" where");
		sb.append(" e.EXPERIMENT_ID = p.EXPERIMENT_ID");
		sb.append(" and e.EXPERIMENT_ID in (${experimentIds})");
		
		addPlateFilters(sb, settings);
		
		sb.append(" order by p.PLATE_ID");
		
		String sql = sb.toString();
		
		// Insert the experiment IDs.
		sql = sql.replace("${experimentIds}", settings.getExperiments().stream()
				.map((experiment) -> Long.toString(experiment.getId()))
				.collect(Collectors.joining(",")));
		
		Query query = new Query();
		query.setSql(sql);
		return query;
	}

	private void addPlateFilters(StringBuilder sb, IFilterPlatesSettings settings) {
		if (settings.getFilterValidation()) {
			Date from = settings.getValidationDateFrom();
			Date to = settings.getValidationDateTo();
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

			String user = settings.getValidationUser();
			if (user != null && !user.isEmpty()) {
				sb.append(" AND P.VALIDATE_USER LIKE '%" + user + "%'");
			}
		}

		if (settings.getFilterApproval()) {
			Date from = settings.getApprovalDateFrom();
			Date to = settings.getApprovalDateTo();
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

			String user = settings.getApprovalUser();
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
