load("script://dc/common.js");

forEachReading(function(reading) {
	var barcode = reading.getBarcode();
	var parentFolderPath = API.get("FileUtils").getPath(reading.getSourcePath());
	var parentFolderName = API.get("FileUtils").getName(parentFolderPath);
	barcode = barcode + "_" + parentFolderName.substring(0, 4);
	reading.setBarcode(barcode);
});