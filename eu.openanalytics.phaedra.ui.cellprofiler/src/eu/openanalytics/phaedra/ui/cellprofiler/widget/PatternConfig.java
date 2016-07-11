package eu.openanalytics.phaedra.ui.cellprofiler.widget;

import java.util.Arrays;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.CollectionUtils;

public class PatternConfig {
	
	public String pattern;
	public GroupRole[] groupRoles;
	public String folder;
	public boolean patternEditable;
	public boolean groupsEditable;
	public boolean folderEditable;
	
	public PatternConfig() {
		pattern = ".*";
		groupRoles = new GroupRole[] { GroupRole.None, GroupRole.None, GroupRole.None };
		folder = ".";
		patternEditable = true;
		groupsEditable = false;
		folderEditable = false;
	}
	
	public enum GroupRole {
		None,
		WellId,
		WellRow,
		WellColumn,
		FieldNr
	}
	
	public static String serializeRoles(GroupRole[] roles) {
		return Arrays.stream(roles).map(r -> r.toString()).collect(Collectors.joining("::"));
	}
	
	public static GroupRole[] deserializeRoles(String roles) {
		return Arrays.stream(roles.split("::")).map(s -> GroupRole.valueOf(s)).toArray(i -> new GroupRole[i]);
	}
	
	public static String toIdGroupString(GroupRole[] roles) {
		StringBuilder sb = new StringBuilder();
		int idIndex = CollectionUtils.find(roles, GroupRole.WellId);
		int rowIndex = CollectionUtils.find(roles, GroupRole.WellRow);
		int colIndex = CollectionUtils.find(roles, GroupRole.WellColumn);
		
		if (idIndex >= 0) {
			sb.append(idIndex+1);
		} else if (rowIndex >=0 && colIndex >= 0) {
			sb.append(rowIndex+1);
			sb.append(",");
			sb.append(colIndex+1);
		} else {
			return null;
		}
		
		return sb.toString();
	}
	
	public static String toFieldGroupString(GroupRole[] roles) {
		int fieldIndex = CollectionUtils.find(roles, GroupRole.FieldNr);
		if (fieldIndex >= 0) {
			return "" + (fieldIndex+1);
		} else {
			return null;
		}
	}
}