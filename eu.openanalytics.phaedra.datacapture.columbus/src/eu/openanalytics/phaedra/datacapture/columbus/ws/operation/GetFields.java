package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetFields.Field;

public class GetFields extends BaseListOperation<Field> {

	private long wellId;
	private long measurementId;
	
	public GetFields(long wellId, long measurementId) {
		this.wellId = wellId;
		this.measurementId = measurementId;
	}
	
	@Override
	protected String[] getOperationParameters() {
		return new String[] { "wellId", ""+wellId, "measurementId", ""+measurementId };
	}
	
	@Override
	protected Class<? extends Field> getObjectClass() {
		return Field.class;
	}
	
	public static class Field {
		public long fieldId;
		public int field;
		public double posX;
		public double posY;
		public long imageId;
	}
}
