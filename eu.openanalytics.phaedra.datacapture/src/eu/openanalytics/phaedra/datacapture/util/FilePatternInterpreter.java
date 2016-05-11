package eu.openanalytics.phaedra.datacapture.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilePatternInterpreter {

	private String regex;
	private Pattern pattern;
	private int[] idGroups;
	private int fieldGroup;
	
	public FilePatternInterpreter(String regex, String idGroupsCfg, String fieldGroupCfg) {
		this.regex = regex;
		this.pattern = Pattern.compile(regex);
		
		if (idGroupsCfg == null || idGroupsCfg.isEmpty()) {
			idGroups = null;
		} else {
			String[] ids = idGroupsCfg.split(",");
			idGroups = new int[ids.length];
			for (int i=0; i<ids.length; i++) {
				idGroups[i] = Integer.parseInt(ids[i]);
			}
		}
		
		if (fieldGroupCfg == null || fieldGroupCfg.isEmpty()) {
			fieldGroup = -1;
		} else {
			fieldGroup = Integer.parseInt(fieldGroupCfg);
		}
	}
	
	public PatternMatch match(String name) {
		Matcher matcher = pattern.matcher(name);
		
		PatternMatch match = new PatternMatch(false, matcher, null, -1);
		
		// Locate the id group(s)
		if (matcher.matches()) {
			match.isMatch = true;
			int groups = matcher.groupCount();
			
			if (idGroups == null) {
				// No id groups specified. Attempt to use defaults.
				if (groups == 2) {
					match.id = "R" + matcher.group(1) + "-C" + matcher.group(2);
				} else if (groups > 0) {
					match.id = matcher.group(1);
				}
			} else {
				if (idGroups.length == 1 && groups >= idGroups[0]) {
					match.id = matcher.group(idGroups[0]);
				} else if (idGroups.length > 1 && groups >= idGroups[0] && groups >= idGroups[1]) {
					match.id = "R" + matcher.group(idGroups[0]) + "-C" + matcher.group(idGroups[1]);
				}
			}
			
			if (fieldGroup != -1 && groups >= fieldGroup) {
				String fieldString = matcher.group(fieldGroup);
				try { match.field = Integer.parseInt(fieldString); } catch (NumberFormatException e) {}
			}
		}
		
		return match;
	}
	
	public String getRegex() {
		return regex;
	}
	
	public Pattern getPattern() {
		return pattern;
	}
	
	public static class PatternMatch {
		
		public boolean isMatch;
		public Matcher matcher;
		public String id;
		public int field;
		
		public PatternMatch() {
			// Default constructor
		}
		
		public PatternMatch(boolean isMatch, Matcher matcher, String id, int field) {
			this.isMatch = isMatch;
			this.matcher = matcher;
			this.id = id;
			this.field = field;
		}
	}
}
