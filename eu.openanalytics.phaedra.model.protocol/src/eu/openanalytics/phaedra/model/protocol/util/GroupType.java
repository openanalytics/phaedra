package eu.openanalytics.phaedra.model.protocol.util;

public enum GroupType {

	WELL(0)
	, SUBWELL(1);
	
	private int type;
	
	private GroupType(int type) {
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
	
	public static GroupType getGroupType(int type) {
		for (GroupType groupType : values()) {
			if (groupType.type == type) {
				return groupType;
			}
		}
		return null;
	}
	
}