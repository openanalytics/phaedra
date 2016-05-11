load("script://dc/common.js");

var basePath = getParameter("file.path", ".");
var filePattern = getParameter("file.pattern", "Meas_(\\d+)\\.xml");

var plateRowsXPath = "/Measurement/PlateLayout/PlateDescription/@Rows";
var plateColsXPath = "/Measurement/PlateLayout/PlateDescription/@Columns";

forEachReading(function(reading) {

	// Locate the measurement file
	var measFilePath = findFile(basePath, filePattern, false);

	// Parse the plate dimensions from the meas file
	var doc = parseXMLFile(measFilePath);
	var plateRows = parseInt(getParameter("plate.rows", "0"));
	var plateCols = parseInt(getParameter("plate.columns", "0"));
	if (plateRows == 0) plateRows = API.get("XmlUtils").findString(plateRowsXPath, doc);
	if (plateCols == 0) plateCols = API.get("XmlUtils").findString(plateColsXPath, doc);
	reading.setRows(plateRows);
	reading.setColumns(plateCols);

	// Set the meas file path as a shared parameter (e.g. so the montage module can use it)
	setSharedParameter("reading.measFilePath", measFilePath);

});

// Take the InstanceFileShare parameter and set it as a parameter "image.path"
var imagePath = getParameter("InstanceFileShare");
if (imagePath === undefined) {
	imagePath = eu.openanalytics.phaedra.datacapture.columbus.prefs.Prefs.getDefaultLogin().fileShare;
}
task.getParameters().put("image.path", imagePath);
