package eu.openanalytics.phaedra.base.security.model;

public enum AccessScope {
	
	/*
	 * Keep in mind that when adding additional scopes they might become available for Objects that were not intended to have these.
	 * E.g. Silos (CreateSiloDialog.java uses AccessScope.values())
	 */
	
	PRIVATE("Private", AccessScope.GROUP_PRIVATE, 0)
	, TEAM("Team", AccessScope.GROUP_TEAM, 10)
	, PUBLIC_R("Public Read", AccessScope.GROUP_PUBLIC, 20)
	, PUBLIC_RU("Public Read/Update", AccessScope.GROUP_PUBLIC, 25)
	, PUBLIC_RUD("Public Read/Update/Delete", AccessScope.GROUP_PUBLIC, 29)
	; 
	
	private static final byte GROUP_PRIVATE = 0;
	private static final byte GROUP_TEAM = 1;
	private static final byte GROUP_PUBLIC = 2;
	
	private String name;
	private byte groupId;
	private int restriction;

	private AccessScope(String name, byte groupId, int restriction) {
		this.name = name;
		this.groupId = groupId;
		this.restriction = restriction;
	}
	
	public String getName() {
		return name;
	}

	public boolean isPublicScope() {
		return GROUP_PUBLIC == groupId;
	}
	
	public boolean isTeamScope() {
		return GROUP_TEAM == groupId;
	}
	
	public boolean isPrivateScope() {
		return GROUP_PRIVATE == groupId;
	}
	
	public static String[] getScopeNames() {
		String[] shareOptions = new String[AccessScope.values().length];
		int index = 0;
		for (AccessScope scope : AccessScope.values()) {
			shareOptions[index++] = scope.getName();
		}
		return shareOptions;
	}
	
	public static int getScopeIndex(AccessScope accessScope) {
		int index = 0;
		for (AccessScope scope : values()) {
			if (scope.equals(accessScope)) {
				return index;
			}
			index++;
		}
		return index;
	}
	
	public boolean isLessRestrictiveThan(AccessScope scope) {
		return restriction >= scope.restriction;
	}
	
}