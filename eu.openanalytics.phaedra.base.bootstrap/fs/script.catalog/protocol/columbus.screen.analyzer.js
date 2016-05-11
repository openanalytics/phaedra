/*
 * This script is called by the Columbus Protocol Wizard, to analyze the contents of a screen.
 * The "screen" object is passed as an argument into this script.
 * It is expected to fill these lists, which are passed into the script:
 * 
 * - wellFeatures: The list of well features
 * - subwellFeatures: The list of subwell features
 * - imageChannels: The list of image channels
 * - imageChannelPatterns: The list of file patterns of each image channel (type String)
 * - imageChannelThumbs: (optional) A list of image thumbs (type org.eclipse.swt.ImageData)
 * 
 * In addition, a map named "parameters" is available that can be used to store additional information
 * that needs to be passed to the template creator script.
 * 
 * Approach:
 * First, the script will connect to Columbus, and retrieve a sample resultset from the selected screen.
 * If there is none, the script will fail with an error message.
 * What happens next depends on the configurations specified below. Each configuration represents a supported
 * combination of file locations and types.
 * 
 * E.g. the 'mchf' configuration assumes that the MChF analysis script has been used in Columbus. As a result,
 * images and subwell data files have been montaged and stored on an external shared drive.
 * The 'columbusOnly' configuration assumes no data is stored on external shares. As a consequence, there is
 * no subwell data or image overlays. Well features and raw images are taken from Columbus directly, and montaged
 * as needed.
 */

var configurations = {
		mchf: {
			wellDataParser: "acapella.welldata.parser",
			subwellDataPattern: "[wW]ell([A-Z]+\\d+)_Detail\\.txt",
			subwellDataParser: "txt.subwelldata.parser",
			imagePattern: "[wW]ell([A-Z]+\\d+)_AllFields_(.*)\\.tiff?",
			imagePatternGroups: ["id", "channel"],
			imageSourceColumbus: false
		},
		columbusOnly: {
			wellDataParser: "acapella.welldata.parser",
			imageSourceColumbus: true
		}
};

var externalPath = API.get("ColumbusService").getInstance(instanceId).fileShare;
var client = API.get("ColumbusService").connect(instanceId);

var rawChannelColors = [ [128,255,255], [255,0,128], [0,255,0], [255,0,0], [0,0,255] ];
var overlayChannelColors = [ [255,255,255], [255,255,0], [128,255,255] ];

var tempDir = API.get("FileUtils").generateTempFolder(true);

try {
	monitor.subTask("Retrieving sample resultset");
	var sample = getSampleResult();
	if (sample == null) throw "Invalid screen selected: this screen has no analysis result sets";
	
	for (configName in configurations) {
		if (tryConfiguration(configurations[configName])) break;
	}
} finally {
	API.get("FileUtils").deleteRecursive(tempDir);
}

function getSampleResult() {
	var plates = API.get("ColumbusService").getPlates(client, screen.screenId).toArray();
	for (var i in plates) {
		var plate = plates[i];
		var measurements = API.get("ColumbusService").getMeasurements(client, screen.screenId, plate.plateId).toArray();
		for (var j in measurements) {
			var meas = measurements[j];
			var res = API.get("ColumbusService").getLatestResult(client, meas.measurementId);
			if (res != null) return [ meas, res ];
		}
	}
	return null;
};

function tryConfiguration(config) {
	wellFeatures.clear();
	subwellFeatures.clear();
	imageChannels.clear();
	imageChannelPatterns.clear();
	imageChannelThumbs.clear();
	
	getWellFeatures(config);
	
	if (config.imageSourceColumbus == true) getColumbusImageChannels();
	
	var externalMeasPath = null;
	if (externalPath != null) {
		var screenPath = java.nio.file.Paths.get(externalPath + "\\" + screen.screenName);
		if (screenPath.toFile().isDirectory()) externalMeasPath = java.nio.file.Files.list(screenPath).findAny().orElse(null);
	}
	
	if (externalMeasPath != null) {
		if (config.subwellDataPattern !== undefined) getSubwellFeatures(config, externalMeasPath);
		if (config.imagePattern !== undefined) getExternalImageChannels(config, externalMeasPath);
	}
	
	var success = !imageChannels.isEmpty();
	if (success) {
		if (config.subwellDataPattern !== undefined) parameters.put("subwelldata.filepattern", config.subwellDataPattern);
		if (externalMeasPath!= null) parameters.put("externalPath", externalPath);
	}
	return success;
}

function getWellFeatures(config) {
	monitor.subTask("Retrieving well features");
	
	var resultValue = API.get("ColumbusService").getResultData(client, sample[1].resultId);
	if (resultValue == null) throw "Invalid screen selected: cannot download result set data";
	
	var model = API.get("ParserService").parse(resultValue.getBytes(), config.wellDataParser);
	if (model.getPlate(0).getWells().length == 0) return;
	var well = model.getPlate(0).getWells()[0];
	var features = well.getFeatureIds();
	java.util.Arrays.sort(features);
	for (var i in features) {
		wellFeatures.add(createFeature(features[i], false, true));
	}
}

function getSubwellFeatures(config, externalMeasPath) {
	monitor.subTask("Retrieving subwell features");
	var pattern = java.util.regex.Pattern.compile(config.subwellDataPattern);
	var match = java.nio.file.Files.list(externalMeasPath)
		.map(function(f) { return pattern.matcher(f.getFileName().toString()) })
		.filter(function(m) { return m.matches() })
		.findAny().orElse(null);
	if (match != null) {
		var dataFile = externalMeasPath.toString() + "\\" + match.group();
		var model = API.get("ParserService").parse(dataFile, config.subwellDataParser);
		var well = model.getPlate(0).getWells()[0];
		var features = well.getSubWellData().keySet().toArray();
		java.util.Arrays.sort(features);
		for (var i in features) {
			subwellFeatures.add(createFeature(features[i], false, true));
		}
	}
}

function getColumbusImageChannels() {
	monitor.subTask("Retrieving image channels from Columbus");
	var sampleField = null;
	var imageInfo = null;
	
	var wells = API.get("ColumbusService").getWells(client, sample[0].measurementId).toArray();
	for (var i in wells) {
		var well = wells[i];
		var fields = API.get("ColumbusService").getFields(client, well.wellId, sample[0].measurementId).toArray();
		for (var j in fields) {
			var field = fields[j];
			imageInfo = API.get("ColumbusService").getImageInfo(client, field.imageId);
			sampleField = field;
			break;
		}
		if (imageInfo != null) break;
	}
	if (imageInfo == null) return;
	
	var imagePath = tempDir + "/sample-field.tif";
	try {
		var out = new java.io.FileOutputStream(imagePath);
		API.get("ColumbusService").getImage(client, sampleField.imageId, out);
	} finally {
		if (out != null) out.close();
	}
	var imageDatas = API.get("TIFFCodec").read(imagePath);

	for (var i=0; i<imageInfo.channels; i++) {
		imageChannels.add(createChannel("Channel " + (i+1), imageDatas[i].depth, false, "columbus", true, rawChannelColors[i%rawChannelColors.length]));
		imageChannelPatterns.add("(.*)_Field(\\d+)_Ch" + (i+1) + "\\.tiff?");
		imageChannelThumbs.add(createThumb(imageDatas[i]));
	}
}

function getExternalImageChannels(config, externalMeasPath) {
	monitor.subTask("Retrieving image channels from external location");
	
	var pattern = java.util.regex.Pattern.compile(config.imagePattern);
	var channelGroup = config.imagePatternGroups.indexOf("channel") + 1;
	var fieldGroup = config.imagePatternGroups.indexOf("field") + 1;
	
	var matches = java.nio.file.Files.list(externalMeasPath)
		.map(function(f) { return pattern.matcher(f.getFileName().toString()) })
		.filter(function(m) { return m.matches() })
		.collect(java.util.stream.Collectors.groupingBy(function(m) { return m.group(channelGroup) }));
	
	// For each name, parse a sample image to obtain: depth, type, thumb
	var imageNames = matches.keySet().toArray();
	for (var i in imageNames) {
		var matcher = matches.get(imageNames[i]).get(0);
		var sample = matcher.group();
		var imagePath = externalMeasPath.toString() + "\\" + sample;
		
		var extension = API.get("FileUtils").getExtension(sample).toLowerCase();
		var imageData = extension.equals("tif") ? API.get("TIFFCodec").read(imagePath)[0] : new org.eclipse.swt.graphics.ImageLoader().load(imagePath)[0];

		var isOverlay = false;
		if (sample.toLowerCase().contains("outlines")) isOverlay = true;
		if (extension.equals("png")) isOverlay = true;
		if (imageData.depth == 1) isOverlay = true;
		var colorArray = isOverlay ? overlayChannelColors : rawChannelColors;
		
		var montage = (fieldGroup > 0);
		imageChannels.add(createChannel(imageNames[i], imageData.depth, isOverlay, "external", montage, colorArray[i%colorArray.length]));
		
		var pattern = matcher.pattern().pattern();
		if (montage) pattern = pattern.replace("(0*1)", "(\\d+)");
		pattern = pattern.replace("(.*)", imageNames[i]);
		imageChannelPatterns.add(pattern);
		
		imageChannelThumbs.add(createThumb(imageData));
	}
};

function createThumb(imageData) {
	var rect = new org.eclipse.swt.graphics.Rectangle(100, 100, 500, 500);
	rect.width = Math.min(500, imageData.width - 100);
	rect.height = Math.min(500, imageData.height - 100);
	var thumb = API.get("ImageUtils").crop(imageData, rect);
	return thumb;
}

function createChannel(name, depth, isOverlay, source, montage, rgb) {
	var ch = API.get("ProtocolService").createChannel(null);
	ch.setName(name);
	ch.setSequence(imageChannels.size() + 1);
	ch.setColorMask(API.get("ColorUtils").rgbToHex(new org.eclipse.swt.graphics.RGB(rgb[0],rgb[1],rgb[2])));
	ch.setBitDepth(depth);
	ch.setLevelMax(Math.pow(2, depth) - 1);
	ch.getChannelConfig().put("source", source);
	ch.getChannelConfig().put("montage", "" + montage);
	if (depth == 16) ch.setLevelMax(4000);
	if (isOverlay) ch.setType(1);
	return ch;
}

function createFeature(name, key, numeric) {
	var feature = API.get("CaptureUtils").newFeatureDef(name);
	feature.isKey = key;
	feature.isNumeric = numeric;
	return feature;
}