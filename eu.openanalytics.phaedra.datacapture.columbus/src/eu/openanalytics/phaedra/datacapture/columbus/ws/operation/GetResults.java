package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import java.util.Date;

import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetResults.Result;

public class GetResults extends BaseListOperation<Result> {

	private long measurementId;
	
	public GetResults(long measurementId) {
		this.measurementId = measurementId;
	}
	
	@Override
	protected String[] getOperationParameters() {
		return new String[] { "measurementId", ""+measurementId };
	}
	
	@Override
	protected Class<? extends Result> getObjectClass() {
		return Result.class;
	}
	
	public static class Result {
		public long resultId;
		public String resultName;
		public Date resultDate;
		public long analysisId;
	}
}
