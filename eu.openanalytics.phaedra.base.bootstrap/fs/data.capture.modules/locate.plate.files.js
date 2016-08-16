load("script://dc/common.js");

/*
 * This module takes an input folder and scans it for the presence of plate files.
 * For each plate file, a new PlateReading will be instantiated and added to the DataCaptureContext.
 *  
 * Required parameters:
 *  
 * plate.pattern - A regular expression specifying the pattern a file must match in order to be considered a plate file.
 * plate.barcode.group - A number indicating the group within plate.pattern that represents the plate barcode.
 */
 
monitor.beginTask("Locating plate files", 100);

var pattern = getParameter("plate.pattern", ".*");
var barcodeGroup = getParameter("plate.barcode.group", 1);
var sourcePath = task.getSource();

var matchingPaths = findFiles(sourcePath, pattern);

for (var i=0; i<matchingPaths.length; i++) {
	var path = matchingPaths[i];
	var file = new java.io.File(path);
	if (file.isFile()) {
		var fileName = file.getName();
		var reading = ctx.createNewReading(i+1);
		reading.setSourcePath(path);
		var barcode = matchPattern(fileName, pattern, barcodeGroup);
		reading.setBarcode(barcode);
	}
	
	monitor.worked(100/matchingPaths.length);
}

ctx.getLogger().info(ctx.getReadings().length + " reading(s) found in " + sourcePath);
monitor.done();