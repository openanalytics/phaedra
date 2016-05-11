load("script://dc/common.js");

var filePath = getParameter("file.path", ".");
var filePattern = getParameter("file.pattern");
var parserId = getParameter("parser.id");
var isOptional = getParameter("optional", false);

forEachReading(function(reading) {
	// Locate the file.
	var dataFile = findFile(filePath, filePattern, isOptional);
	reading.setFileName(API.get("FileUtils").getName(dataFile));

	// Parse the file.
	var model = parseFile(dataFile, parserId);
	
	// Save the parsed model.
	// Use the plate dimensions given by the data file.
	saveModel(model);
	reading.setRows(model.getPlate(0).getRows());
	reading.setColumns(model.getPlate(0).getColumns());
});