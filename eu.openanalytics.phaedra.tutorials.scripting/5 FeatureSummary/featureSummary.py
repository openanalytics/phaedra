import math

plateId = int(str(args.get(0)))

plate = API.get("PlateService").getPlateById(plateId)
dataAccessor = API.get("CalculationService").getAccessor(plate)
wellCount = plate.getRows() * plate.getColumns()
features = API.get("ProtocolUtils").getFeatures(plate).toArray()

# Calculate a summary for each numeric well feature
summary = {}
for feature in features:
	fName = feature.getName()
	if (feature.isNumeric()):
		summary[fName] = { "Min": float("inf"), "Max": float("-inf") }
		
		for nr in range(1, wellCount + 1):
			value = dataAccessor.getNumericValue(nr, feature, None)
			if (math.isnan(value)):
				continue
			summary[fName]["Min"] = min(summary[feature.getName()]["Min"], value)
			summary[fName]["Max"] = max(summary[feature.getName()]["Max"], value)

# Print the summary in the console
print "Summary: feature, min, max"
for feature in features:
	fName = feature.getName()
	if (feature.isNumeric()):
		print fName + "\t" + str(summary[fName]["Min"]) + "\t" + str(summary[fName]["Max"])