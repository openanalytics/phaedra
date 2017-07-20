load("script://dc/common.js");

/**
 * Parse numerical subwell data from a text file.
 * If the parsed data belongs to a single well whose position
 * cannot be calculated, it will be stored in well A1 of the model.
 */

var colSep = params.get("column.separator") || "\t";
var commentSign = params.get("comment.sign") || "#";
var wellIdColName = params.get("wellid.column");
var wellIdPattern = params.get("wellid.pattern");
if (wellIdPattern != null) wellIdPattern = java.util.regex.Pattern.compile(wellIdPattern);

var lines = API.get("ParserUtils").toLines(data, false);
var headers = null;
var dataLines = [];

for (var i in lines) {
	// Ignore comment lines
	if (lines[i].startsWith(commentSign)) continue;
	
	var cells = lines[i].split(colSep);
	if (cells.length == 0) continue;
	if (cells.length == 2) continue; // Support old files containing key-value pairs at the beginning
	
	// The first non-comment line contains the headers
	if (headers == null) {
		headers = cells;
		trimStrings(headers);
		continue;
	}
	
	// The remaining lines contain data
	dataLines.push(cells);
}
lines = null;

// Determine the plate dimensions by finding the highest well nr.
var plateDims = null;
var wellIdCol = (wellIdColName == null) ? null : findString(headers, wellIdColName, false);
if (wellIdCol == null) {
	plateDims = [1, 1];
} else {
	var maxPos = [0, 0];
	var maxNr = 0;
	for (var i in dataLines) {
		var wellId = parseWellId(dataLines[i][wellIdCol]);
		if (API.get("NumberUtils").isNumeric(wellId)) {
			maxNr = parseInt(wellId);
		} else {
			maxPos[0] = Math.max(maxPos[0], API.get("NumberUtils").convertToRowNumber(wellId));
			maxPos[1] = Math.max(maxPos[1], API.get("NumberUtils").convertToColumnNumber(wellId));
		}
	}
	if (maxNr == 0) maxNr = maxPos[0] * maxPos[1];
	plateDims = calculatePlateSize(maxNr, maxPos[1], true);
}
var plate = API.get("ModelUtils").createPlate(model, plateDims[0], plateDims[1]);

// Group the data per well, in case the file contains data for multiple wells.
var dataPerWell = {};
for (var i in dataLines) {
	var well = getWell(dataLines[i]);
	if (dataPerWell[well] == null) dataPerWell[well] = [];
	dataPerWell[well].push(dataLines[i]);
}
dataLines = null;

// Add all data to the datasets
for (var r=0; r<plateDims[0]; r++) {
	for (var c=0; c<plateDims[1]; c++) {
		var well = plate.getWell(r+1, c+1);
		if (dataPerWell[well] == null) continue;
		
		for (var i in headers) {
			if (headers[i].isEmpty()) continue;
			var ds = API.get("ModelUtils").newSubWellDataset(dataPerWell[well].length, true, headers[i], well);
			for (var j=0; j<dataPerWell[well].length; j++) {
				ds.addNumericValue(j, parseFloat(dataPerWell[well][j][i]));
			}
		}
		
		dataPerWell[well] = null;
	}
}

function getWell(dataLine) {
	if (wellIdCol == null) return plate.getWell(1, 1);
	
	var wellId = parseWellId(dataLine[wellIdCol]);
	var pos = null;
	if (API.get("NumberUtils").isNumeric(wellId)) {
		pos = API.get("NumberUtils").getWellPosition(parseInt(wellId), plate.getColumns());
	} else {
		pos = [ API.get("NumberUtils").convertToRowNumber(wellId), API.get("NumberUtils").convertToColumnNumber(wellId) ];
	}
	return plate.getWell(pos[0], pos[1]);
}

function parseWellId(wellId) {
	if (wellIdPattern == null) return wellId;
	var m = wellIdPattern.matcher(wellId);
	if (m.matches()) {
		if (m.groupCount() == 1) return m.group(1);
		else if (m.groupCount() == 2) return "R" + m.group(1) + "C" + m.group(2);
	}
	return wellId;
}