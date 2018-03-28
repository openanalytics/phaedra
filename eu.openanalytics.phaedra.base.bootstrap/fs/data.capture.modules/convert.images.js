load("script://dc/common.js");

var srcPath = getParameter("source.path", ".");
var srcPattern = getParameter("source.pattern");
var destPathVar = getParameter("destination.path.var", "reading.image.path.converted");
var destFormat = getParameter("destination.format", "tif");
var convertArgs = getParameter("convert.args", "");

var converter = Java.type("eu.openanalytics.phaedra.base.imaging.util.IMConverter");

var tempFolders = [];

forEachReading(function(reading) {
	var files = findFiles(srcPath, srcPattern);
	ctx.getLogger().info(reading, "Images found to convert: " + files.length);

	var tempFolder = createTempFolder();
	setParameter(destPathVar, tempFolder);

	var fileArray = [];
	for (var i=0; i<files.length; i++) {
		fileArray.push([reading, files[i], tempFolder]);
	}
	forEachMT(fileArray, convertImage);
});

function convertImage(args) {
	if (monitor.isCanceled()) return;
	var reading = args[0];
	var image = args[1];
	var destPath = args[2];
	var imageName = API.get("FileUtils").getName(image);
	if (imageName.contains(".")) imageName = imageName.substring(0, imageName.lastIndexOf("."));
	var destination = API.get("CaptureUtils").resolvePath(destPath, reading.getSourcePath(), ctx) + "/" + imageName + "." + destFormat;
	retrying(function(){ converter.convert(image, convertArgs, destination); }, 10, 2000);
}

function createTempFolder() {
	tempFolder = API.get("FileUtils").generateTempFolder(true);
	tempFolders.push(tempFolder);
	return tempFolder;
}

function retrying(func, maxTries, waitTime) {
	var currentTry = 1;
	var caughtException = null;
	while (currentTry <= maxTries) {
		try {
			func();
			console.print("Execution succeeded on try " + currentTry + "/" + maxTries);
			return;
		} catch (err) {
			console.print("Execution error, retrying: " + currentTry + "/" + maxTries);
			caughtException = err;
			if (waitTime > 0) {
				try { java.lang.Thread.sleep(waitTime); } catch (wakeUpErr) {}
			}
			currentTry++;
		}
	}
	throw caughtException;
}

function postCapture() {
	for (i in tempFolders) API.get("FileUtils").deleteRecursive(tempFolders[i]);
}
setParameter(ctx.getActiveModule().getId() + ".postCapture", postCapture);