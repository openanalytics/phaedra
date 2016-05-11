package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetWells.Well;

public class GetWells extends BaseListOperation<Well> {

	private long measurementId;
	
	public GetWells(long measurementId) {
		this.measurementId = measurementId;
	}
	
	@Override
	protected String[] getOperationParameters() {
		return new String[] { "measurementId", ""+measurementId };
	}
	
	@Override
	protected Class<? extends Well> getObjectClass() {
		return Well.class;
	}
	
	public static class Well {
		public String wellName;
		public long wellId;
		public int row;
		public int column;
		public int numberOfFields;
	}
}
