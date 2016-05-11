
/**
 * A parser receives source data (usually from a file), and parses it into a
 * well-known model for Phaedra to capture and import.
 * 
 * The following objects are available to the script:
 * 
 * API			- The Phaedra scripting API, type API.getServices() for more information
 * data			- The data to parse, given as a byte array
 * dataStream	- The data to parse, given as a java.io.InputStream
 * model		- An instance of eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel
 * params		- A Map containing parameters passed to the parser
 */

var plate = API.get("ModelUtils").createPlate(model, 16, 24);

for (var r = 1; r <= plate.getRows(); r++) {
	for (var c = 1; c <= plate.getColumns(); c++) {
		var well = plate.getWell(r, c);
		
		// Add a well feature to the model
		var featureValue = API.get("ModelUtils").newFeature("Example feature", well);
		featureValue.setNumericValue(Math.random());
		
		// Add a subwell feature to the model
		var cellCount = 1000;
		var swDataset = API.get("ModelUtils").newSubWellDataset(cellCount, true, "Example subwell feature", well);
		for (var i = 0; i <= cellCount; i++) {
			swDataset.addNumericValue(i, Math.random());
		}
	}
}
