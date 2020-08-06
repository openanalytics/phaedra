load("script://web.api/header.js");

var dataType = urlParts[0].toLowerCase();
if (dataType != "raw" && dataType != "norm") throw "Invalid argument: " + urlParts[0];

var plateId = parseInt(urlParts[1]);
var plate = API.get("PlateService").getPlateById(plateId);
if (plate == null) {
	http.replyError(response, 404, "Plate not found for ID " + plateId);
} else {
	var accessor = API.get("CalculationService").getAccessor(plate);
	if (urlParts.length > 2) {
		fId = urlParts[2];
		if (fId.contains(",")) {
			fIds = fId.split(",");
			features = fIds.map(function(i) { return API.get("ProtocolService").getFeature(parseInt(i)); });
		} else {
			features = [ API.get("ProtocolService").getFeature(parseInt(fId)) ];
		}
	} else {
		features = API.get("ProtocolUtils").getFeatures(plate).toArray();
		accessor.loadEager(null);
	}

	var wellCount = plate.getWells().size();
	var welldata = {};
	for (var i in features) {
		var f = features[i];
		var data = [];
		for (var j=0; j<wellCount; j++) {
			if (f.isNumeric()) {
				data[j] = accessor.getNumericValue(j+1, f, (dataType == "raw") ? null : f.getNormalization());
				// JSON has no representation for NaN or Infinity -> send null
				if (java.lang.Double.isNaN(data[j])) data[j] = null;
			} else {
				data[j] = accessor.getStringValue(j+1, f);
			}
		}
		welldata[f.getId()] = data;
	}

	http.replyOk(response, welldata);
}