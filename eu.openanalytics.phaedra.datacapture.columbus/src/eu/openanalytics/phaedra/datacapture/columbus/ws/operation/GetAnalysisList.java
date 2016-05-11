package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import java.util.Date;

import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetAnalysisList.Analysis;

public class GetAnalysisList extends BaseListOperation<Analysis> {

	private long measurementId;
	
	public GetAnalysisList(long measurementId) {
		this.measurementId = measurementId;
	}
	
	@Override
	protected String[] getOperationParameters() {
		return new String[] { "measurementId", ""+measurementId };
	}
	
	@Override
	protected String getOperationName() {
		return "getAnalysisList";
	}
	
	@Override
	protected Class<? extends Analysis> getObjectClass() {
		return Analysis.class;
	}
	
	public static class Analysis {
		public long analysisId;
		public String analysisName;
		public Date analysisDate;
	}
}
