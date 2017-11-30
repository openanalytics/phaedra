from java.lang import Float

def saveBlock(featureName, block):
	if (featureName == None) or (len(block) == 0):
		return
	
	# Get the first (and only) plate of this model, or create one if it doesn't exist yet.
	plate = model.getPlate(0)
	if plate == None:
		plate = API.get("ModelUtils").createPlate(model, len(block), len(block[0]))

	for r in range(plate.getRows()):
		for c in range(plate.getColumns()):
			well = plate.getWell(r+1, c+1)
			featureValue = API.get("ModelUtils").newFeature(featureName, well)
			featureValue.setNumericValue(Float(float(block[r][c])))

currentFeature = None
currentBlock = []

lines = API.get("ParserUtils").toLines(data)
for line in lines:
	if (not line) or (line.startswith("#")):
		continue

	columns = line.split("\t")
	if (len(columns) == 1):
		# A new feature starts here
		saveBlock(currentFeature, currentBlock)
		currentFeature = columns[0]
		currentBlock = []
	else:
		# Append a row to the block of the current feature
		currentBlock.append(columns)

saveBlock(currentFeature, currentBlock)