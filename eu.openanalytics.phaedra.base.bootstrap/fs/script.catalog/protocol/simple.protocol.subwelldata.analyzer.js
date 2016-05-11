var patterns = [
                "(?i)well[ _\\-]*([a-zA-Z]+[ _\\-]*\\d+).*",
                "(?i)well[ _\\-]*([Rr]\\d+[ _\\-]*[Cc]\\d+).*",
                "(?i)well[ _\\-]*(\\d+).*"
];

var parsers = [
               "txt.subwelldata.parser"
];

var file = parameters.get("subwelldata.file");
var fileName = API.get("FileUtils").getName(file);

parameters.put("subwelldata.path", API.get("FileUtils").getRelativePath(file, parameters.get("selected.folder")));
for (var i in patterns) {
	if (API.get("CaptureUtils").getMatch(fileName, patterns[i], 0) != null) {
		parameters.put("subwelldata.filepattern", patterns[i]);
	}
}

var parsedModel = null;
for (var i in parsers) {
	try {
		parsedModel = API.get("ParserService").parse(file, parsers[i]);
		parameters.put("subwelldata.parser.id", parsers[i]);
	} catch (e) {
		// Try the next parser
	}
}
if (parsedModel == null) throw "Unsupported file format";