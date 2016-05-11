load("script://dc/common.js");

var filePath = getParameter("file.path", ".");
var filePattern = getParameter("file.pattern", "Results_Analysis\\.xml");
var parserId = getParameter("parser.id", "acapella.welldata.parser");
var isOptional = getParameter("optional", false);

forEachReading(function(reading) {
	// Locate the file.
	var dataFile = findFile(filePath, filePattern, isOptional);
	reading.setFileName(API.get("FileUtils").getName(dataFile));
	
	// Parse the file.
	var model = parseFile(dataFile, parserId);

	// Save the parsed model.
	// Use the plate dimensions given by the meas file, not the res file.
	model.getPlate(0).setRows(reading.getRows());
	model.getPlate(0).setColumns(reading.getColumns());
	ctx.getStore(reading).saveModel(model);
});
