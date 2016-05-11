var patterns = [
                "(?i)well[ _\\-]*([a-zA-Z]+[ _\\-]*\\d+)",
                "(?i)well[ _\\-]*(\\d+)"
];

var file = parameters.get("imagedata.file");
var fileName = API.get("FileUtils").getName(file);
var filePath = API.get("FileUtils").getPath(file);

parameters.put("imagedata.path", API.get("FileUtils").getRelativePath(file, parameters.get("selected.folder")));

for (var i in patterns) {
	if (API.get("CaptureUtils").getMatch(fileName, patterns[i] + "(.*)", 0) != null) {

		var groupNames = new java.util.HashSet();
		var matches = API.get("CaptureUtils").getMatchingChildren(filePath, patterns[i] + "(.*)");
		for (var j in matches) {
			var matchName = API.get("FileUtils").getName(matches[j]);
			var groupName = API.get("CaptureUtils").getMatch(matchName, patterns[i] + "(.*)", 2);
			groupNames.add(groupName);
		}
		
		var sortedGroupNames = new java.util.ArrayList(groupNames);
		java.util.Collections.sort(sortedGroupNames);
		sortedGroupNames = sortedGroupNames.toArray();

		for (var i in sortedGroupNames) {
			parameters.put("imagedata.channel." + (i+1) + ".name", sortedGroupNames[i]);
			parameters.put("imagedata.channel." + (i+1) + ".filepattern", patterns[i] + java.util.regex.Pattern.quote(groupName));
		}
	}
}