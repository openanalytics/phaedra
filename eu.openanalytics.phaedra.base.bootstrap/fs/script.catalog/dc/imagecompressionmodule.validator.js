/*
 * This script is called at the beginning of the image compression module.
 * 
 * Any errors generated here are ignored, i.e. this script cannot interrupt execution of the module.
 * Its purpose is to validate the input and settings, and log warnings if needed.
 * 
 * See ImageCompressionModule.java for more information.
 */

var rows = inputFiles.length;
var columns = rows > 0 ? inputFiles[0].length : 0;
var components = columns > 0 ? inputFiles[0][0].length : 0;

var rt = java.lang.Runtime.getRuntime();
rt.gc();
var bytesUsedArray = new Array();
var bytesUsed = 0;
var bytesAvailable = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
var nrOfThreads = task.getParameters().get("nrOfThreads");

for (var c=0; c<components; c++) {
	var sampleImage = null;
	
	/* Check the number of files found */
	var missingCount = 0;
	for (var x=0; x<inputFiles.length; x++) {
		for (var y=0; y<inputFiles[x].length; y++) {
			if (inputFiles[x][y][c] == null) missingCount++;
			else if (sampleImage == null) sampleImage = inputFiles[x][y][c];
		}
	}
	
	if (missingCount > 0) {
		var expectedCount = rows * columns;
		var cfg = null;
		if (config.components[c].files.length > 0) cfg = config.components[c].files[0];
		
		var msg = "Image component " + c + " : expected " + expectedCount + " images, found "
				+ (expectedCount - missingCount) + " images.\n Path: " + cfg.path + ", pattern: " + cfg.pattern;
		ctx.getLogger().warn(reading, msg);
	}
	
	if (sampleImage != null) {
		var dimensions = API.get("ImageIdentifier").identify(sampleImage);
		bytesUsed += (dimensions[0] * dimensions[1] * dimensions[2] * dimensions[3]) / 8;
		
		/* Test the bitdepth of the images */
		var targetExperiment = task.getParameters().get("experiment");
		if (targetExperiment != null) {
			var convertCmd = config.components[c].convertArgs;
			var isDownscaled = config.components[c].downscale || (convertCmd != null && convertCmd.contains("-depth 8"));
			var sourceDepth = dimensions[3];
			var targetDepth = isDownscaled ? 8 : sourceDepth;
			
			var channels = targetExperiment.getProtocol().getProtocolClass().getImageSettings().getImageChannels();
			var channelIndex = 0;
			for (var i=0; i<c; i++) {
				if (config.components[i].split) {
					channelIndex += config.components[i].splitCount;
				} else {
					channelIndex++;
				}
			}

			var channel = channels.get(channelIndex);
			var channelDepth = channel.getBitDepth() == 0 ? 8 : channel.getBitDepth();
			
			if (targetDepth != channelDepth && (targetDepth != 8 || channelDepth != 1)) {
				ctx.getLogger().warn(reading, "Image component " + c + " : expected bit depth " + channelDepth + " but got bit depth " + targetDepth);	
			}
		}
		
		/* Check for accidental selection of Z-stacks, which are not supported */
		if (sampleImage.contains("_z000_")) {
			// It's a DCI-style image name.
			var testImage = sampleImage.replace("_z000_", "_z001_");
			if (new java.io.File(testImage).exists()) {
				ctx.getLogger().warn(reading, "Image component " + c + " : appears to be a Z stack!");
			}
		}
	}
}

/**
 * Check the nr of threads vs memory available
 */

while (nrOfThreads > 0 && (bytesUsed * nrOfThreads) > bytesAvailable) {
	nrOfThreads--;
}
if (nrOfThreads == 0) {
	ctx.getLogger().warn(reading, "Phaedra is low on memory! " +
			"\nTotal memory available: " + (bytesAvailable / 1024 / 1024).toFixed(2) + " MB. " +
			"\nAvg memory required per well: " + (bytesUsed / 1024 / 1024).toFixed(2) + " MB.");
	nrOfThreads = 1;
}
task.getParameters().put("nrOfThreads", nrOfThreads);

/**
 * Check the amount of temp space needed vs available
 */

var tempFolder = API.get("FileUtils").generateTempFolder(false);
var availableTempSpace = new java.io.File(tempFolder).getParentFile().getFreeSpace();
// Assume: need space for JPX + J2C files + 25%
// Assume: compression factor 2 (worst case) -> JPX + J2C files = bytesUsed
var readingCount = ctx.getReadings().length;
var requiredTempSpace = readingCount*(bytesUsed * 1.25);

if (availableTempSpace < requiredTempSpace) {
	var requiredMB = parseInt(requiredTempSpace/(1024*1024));
	var availableMB = parseInt(availableTempSpace/(1024*1024));
	ctx.getLogger().warn(reading, "Insufficient temporary disk space! Required: " + requiredMB + " MB, available: " + availableMB + " MB");
}