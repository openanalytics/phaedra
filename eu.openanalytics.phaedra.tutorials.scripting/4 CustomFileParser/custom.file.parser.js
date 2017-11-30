var currentFeature = null;
var currentBlock = [];

var lines = API.get("ParserUtils").toLines(data);
for (var i in lines) {
	var line = lines[i];
	if (line.isEmpty() || line.startsWith("#")) continue;

	var columns = line.split("\t");
	if (columns.length == 1) {
		// A new feature starts here
		saveBlock(currentFeature, currentBlock);
		currentFeature = columns[0];
		currentBlock = [];
	} else {
		// Append a row to the block of the current feature
		currentBlock.push(columns);
	}
}
saveBlock(currentFeature, currentBlock);

function saveBlock(featureName, block) {
	if (featureName == null || block.length == 0) return;
	
	// Get the first (and only) plate of this model, or create one if it doesn't exist yet.
	var plate = model.getPlate(0);
	if (plate == null) {
		plate = API.get("ModelUtils").createPlate(model, block.length, block[0].length);
	}
	
	for (var r = 0; r < plate.getRows(); r++) {
		for (var c = 0; c < plate.getColumns(); c++) {
			var well = plate.getWell(r+1, c+1);
			var featureValue = API.get("ModelUtils").newFeature(featureName, well);
			featureValue.setNumericValue(parseFloat(block[r][c]));
		}
	}
};