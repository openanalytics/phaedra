/*
 * Supported formats:
 * - One file per plate (the data contains a well id column) (plate dimensions will be determined by the file contents)
 * - One file per well (the filename contains the well id) (plate dimensions will be taken from reading.getRows() and reading.getColumns())
 */

load("script://dc/common.js");

var parserId = getParameter("parser.id");
var filePath = getParameter("file.path", ".");
var filePattern = getParameter("file.pattern");
var wellIdGroup = getParameter("file.pattern.wellid.group", 1);

forEachReading(function (reading) {
	var files = findFiles(filePath, filePattern);
	ctx.getLogger().info(reading, files.length + " files found matching the pattern '" + filePattern + "'");
	
	forEachMT(files, function(f) {
		if (monitor.isCanceled()) return;
				
		try {
			var model = parseFile(f, parserId);
			var plate = model.getPlate(0);
			if (plate.getWells().length == 1) {
				var well = plate.getWell(1,1);

				var wellNr = matchPattern(API.get("FileUtils").getName(f), filePattern, wellIdGroup);
				if (!API.get("NumberUtils").isNumeric(wellNr)) wellNr = API.get("NumberUtils").getWellNr(wellNr, reading.getColumns());
				var wellPos = API.get("NumberUtils").getWellPosition(wellNr, reading.getColumns());
				
				plate.setRows(reading.getRows());
				plate.setColumns(reading.getColumns());
				plate.removeWell(1,1);
				plate.addWell(wellPos[0], wellPos[1], well);	
			} else if (reading.getRows() == 0 && reading.getColumns() == 0) {
				reading.setRows(plate.getRows());
				reading.setColumns(plate.getColumns());
			}
			saveModel(model);
			delete model;
		} catch (err) {
			doError("Failed to parse subwell data file " + f, err);
		}
	});
});