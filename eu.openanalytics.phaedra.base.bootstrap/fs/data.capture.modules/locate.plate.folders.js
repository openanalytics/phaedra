load("script://dc/common.js");

/*
 * This module takes an input folder and scans it for the presence of 'plate folders'.
 * For each plate folder, a new PlateReading object will be instantiated and added to the DataCaptureContext.
 *
 * Parameters:
 * plate.folder.pattern - A regex pattern that a folder must match in order to be considered a plate folder.
 * plate.folder.barcode.group - The group within the pattern that represents the plate barcode.
 * plate.folder.sequence.group - (Optional) The group within the pattern that represents the plate sequence.
 */

monitor.beginTask("Locating plate folders", 100);

var pattern = getParameter("plate.folder.pattern", ".*");
var barcodeGroup = getParameter("plate.folder.barcode.group", 1);
var sequenceGroup = getParameter("plate.folder.sequence.group");
var plateIds = getParameterAsObject("plateIds");
var sourcePath = task.getSource();
var readingNr = 1;

scanFolder(task.getSource());

// If no readings are found, search 1 level deeper.
if (ctx.getReadings().length == 0) {
	var subFolders = findFolders(sourcePath, ".*");
	for (var i=0; i<subFolders.length; i++) {
		scanFolder(subFolders[i]);
	}
	monitor.worked(100/subFolders.length);
}

// If still no readings are found, assume the source folder IS the reading folder.
if (ctx.getReadings().length == 0) {
	var path = resolveVars(sourcePath);
	createReading(path);
}

ctx.getLogger().info(ctx.getReadings().length + " reading(s) found in " + sourcePath);
monitor.done();

function scanFolder(path) {
	var matchingPaths = findFolders(path, pattern);
	java.util.Arrays.sort(matchingPaths);
	for (var i=0; i<matchingPaths.length; i++) {
		createReading(matchingPaths[i]);
	}
}

function createReading(path) {
	if (plateIds != null) {
		var validId = false;
		for (var p in plateIds) {
			if (path.toLowerCase().startsWith(plateIds[p].toLowerCase())) validId = true;
		}
		if (!validId) return;
	}
	//if (plateIds != null && findString(plateIds, path, false) == -1) return;
	
	var file = new java.io.File(path);
	if (file.isDirectory()) {
		var fileName = file.getName();
		var reading = ctx.createNewReading(readingNr++, file.getAbsolutePath());
		reading.setSourcePath(path);

		var barcode = matchPattern(fileName, pattern, barcodeGroup);
		reading.setBarcode(barcode);
		
		if (sequenceGroup != null) {
			var sequence = matchPattern(fileName, pattern, sequenceGroup);
			if (API.get("NumberUtils").isNumeric(sequence)) reading.setFileInfo(sequence);
		}
	}
}