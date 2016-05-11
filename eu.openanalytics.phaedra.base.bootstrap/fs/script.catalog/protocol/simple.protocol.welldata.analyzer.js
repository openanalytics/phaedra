var parsers = [
               "txt.welldata.parser",
               "excel.welldata.parser"
];

var file = parameters.get("welldata.file");
parameters.put("welldata.path", API.get("FileUtils").getRelativePath(file, parameters.get("selected.folder")));
parameters.put("welldata.filepattern", java.util.regex.Pattern.quote(API.get("FileUtils").getName(file)));

var parsedModel = null;
for (var i in parsers) {
	try {
		parsedModel = API.get("ParserService").parse(file, parsers[i]);
		parameters.put("welldata.parser.id", parsers[i]);
	} catch (e) {
		// Try the next parser
	}
}
if (parsedModel == null) throw "Unsupported file format";