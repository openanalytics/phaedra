package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import java.util.Date;

import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetMeasurements.Measurement;

public class GetMeasurements extends BaseListOperation<Measurement> {

	private long plateId;
	private long screenId;
	
	public GetMeasurements(long plateId, long screenId) {
		this.plateId = plateId;
		this.screenId = screenId;
	}
	
	@Override
	protected String[] getOperationParameters() {
		return new String[] { "plateId", ""+plateId, "screenId", ""+screenId };
	}
	
	@Override
	protected Class<? extends Measurement> getObjectClass() {
		return Measurement.class;
	}
	
	public static class Measurement {
		public Date measurementDate;
		public long measurementId;
	}
}
