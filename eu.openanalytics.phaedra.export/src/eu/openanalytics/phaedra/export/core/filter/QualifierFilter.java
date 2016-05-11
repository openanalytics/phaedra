package eu.openanalytics.phaedra.export.core.filter;

import java.util.List;

import eu.openanalytics.phaedra.export.core.util.SQLUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public class QualifierFilter {

	public final static String ALL = "<All>";
	
	private final static String SELECT = 
		"SELECT DISTINCT"
		+ " EXTRACTVALUE(DATA_XML,'/data/properties/property[@key=\"plate-qualifier\"]/@value') QUALIFIER"
		+ " FROM PHAEDRA.HCA_PLATE WHERE EXPERIMENT_ID IN (${experiment_ids})";
	
	private List<Experiment> experiments;
	
	public QualifierFilter(List<Experiment> experiments) {
		this.experiments = experiments;
	}

	public String[] getQualifiers() {
		String experimentIds = "";
		if (experiments != null) {
			for (Experiment exp: experiments) {
				experimentIds += exp.getId()+",";
			}
			experimentIds = experimentIds.substring(0,experimentIds.lastIndexOf(','));
		}
		
		String sql = SELECT.replace("${experiment_ids}", experimentIds);
		String[] sqlResults = SQLUtils.select(sql, "QUALIFIER", true);
		String[] allResults = new String[sqlResults.length+1];
		allResults[0] = ALL;
		System.arraycopy(sqlResults, 0, allResults, 1, sqlResults.length);
		
		return allResults;
	}	
}
