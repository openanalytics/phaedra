load("script://dc/common.js");

/**
 * Parse numerical subwell data from a text file.
 * The parsed data is stored in well A1 of the model. Multiple wells per file are NOT supported.
 */

var colSep = params.get("column.separator") || "\t";
var commentSign = params.get("comment.sign") || "#";

var lines = API.get("ParserUtils").toLines(data, false);
var headers = null;
var dataLines = [];

for (var i in lines) {
	// Ignore comment lines
	if (lines[i].startsWith(commentSign)) continue;
	
	var cells = lines[i].split(colSep);
	if (cells.length == 0) continue;
	
	// The first non-comment line contains the headers
	if (headers == null) {
		headers = cells;
		trimStrings(headers);
		continue;
	}
	
	// The remaining lines contain data
	dataLines.push(cells);
}

// Define a dataset for each feature column
var well = API.get("ModelUtils").createPlate(model, 1, 1).getWell(1, 1);
var datasets = {};
for (var i in headers) {
	if (headers[i].isEmpty()) continue;
	datasets[headers[i]] = API.get("ModelUtils").newSubWellDataset(dataLines.length, true, headers[i], well);
}

// Add all data to the datasets
for (var i in dataLines) {
	for (var j in headers) {
		if (headers[j].isEmpty()) continue;
		// Anything that fails to parse as a float, becomes NaN
		var value = parseFloat(dataLines[i][j]);
		datasets[headers[j]].addNumericValue(i, value);
	}
}