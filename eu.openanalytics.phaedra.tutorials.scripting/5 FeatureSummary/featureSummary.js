var plateId = parseInt(args[0]);

var plate = API.get("PlateService").getPlateById(plateId);
var dataAccessor = API.get("CalculationService").getAccessor(plate);
var wellCount = plate.getRows() * plate.getColumns();
var features = API.get("ProtocolUtils").getFeatures(plate).toArray();

// Calculate a summary for each numeric well feature
var summary = {};
for (var i in features) {
	var feature = features[i];
	if (feature.isNumeric()) {
		
		summary[feature.getName()] = { "Min": Number.POSITIVE_INFINITY, "Max": Number.NEGATIVE_INFINITY };
		
		for (var nr=1; nr<=wellCount; nr++) {
			var value = dataAccessor.getNumericValue(nr, feature, null);
			if (isNaN(value)) continue;
			summary[feature.getName()]["Min"] = Math.min(summary[feature.getName()]["Min"], value);
			summary[feature.getName()]["Max"] = Math.max(summary[feature.getName()]["Max"], value);
		}
	}
}

// Print the summary in the console
console.print("Summary: feature, min, max");
for (var i in features) {
	var feature = features[i];
	var fName = feature.getName();
	if (feature.isNumeric()) {
		console.print(fName + "\t" + summary[fName]["Min"] + "\t" + summary[fName]["Max"]);
	}
}