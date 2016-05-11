package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetPlates.Plate;

public class GetPlates extends BaseListOperation<Plate> {

	private long screenId;
	
	public GetPlates(long screenId) {
		this.screenId = screenId;
	}
	
	@Override
	protected String[] getOperationParameters() {
		return new String[] { "screenId", ""+screenId };
	}
	
	@Override
	protected Class<? extends Plate> getObjectClass() {
		return Plate.class;
	}
	
	public static class Plate {
		public String plateName;
		public String plateType;
		public long plateId;
	}
}
