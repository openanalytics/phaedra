load("script://dc/common.js");

/**
 * Parse numerical well data from a text file.
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
	
	// The first non-comment line contains the headers
	if (headers == null) {
		headers = cells;
		trimStrings(headers);
		continue;
	}
	
	// The remaining lines contain data
	dataLines.push(cells);
}

// Look for the column containing the well numbers
if (wellIdColName == null) wellIdColName = guessWellColumn(headers);
if (wellIdColName == null) throw "Well id column not found";
var wellIdCol = findString(headers, wellIdColName, false);
if (wellIdCol == -1) throw "Well id column not found: " + wellIdColName;

// Determine the plate dimensions by finding the highest well nr.
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
var plateDims = calculatePlateSize(maxNr, maxPos[1], true);

// Process the data lines into well data
var plate = API.get("ModelUtils").createPlate(model, plateDims[0], plateDims[1]);
for (var i in dataLines) {
	var wellId = parseWellId(dataLines[i][wellIdCol]);
	var pos = null;
	if (API.get("NumberUtils").isNumeric(wellId)) {
		pos = API.get("NumberUtils").getWellPosition(parseInt(wellId), plate.getColumns());
	} else {
		pos = [ API.get("NumberUtils").convertToRowNumber(wellId), API.get("NumberUtils").convertToColumnNumber(wellId) ];
	}
	var well = plate.getWell(pos[0], pos[1]);
	
	for (var j in headers) {
		if (headers[j].isEmpty()) continue;
		var featureValue = API.get("ModelUtils").newFeature(headers[j], well);
		var value = dataLines[i][j];
		if (API.get("NumberUtils").isDouble(value)) featureValue.setNumericValue(parseFloat(value));
		else featureValue.setStringValue(value);		
	}
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