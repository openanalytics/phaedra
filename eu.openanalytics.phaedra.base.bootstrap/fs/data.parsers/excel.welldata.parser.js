load("script://dc/common.js");

/*
 * Parse tabular well data from an XLS(X) file.
 */

var wellIdColName = params.get("wellid.column");
var sheetName = params.get("sheet.name") || "";

var wb = API.get("CaptureUtils").parseExcelWorkbook(dataStream, null);
if (sheetName == "") var sheet = wb.getSheetAt(0);
else var sheet = wb.getSheet(sheetName);

var headers = null;
var dataRows = [];
for (var i=0; i<sheet.getLastRowNum(); i++) {
	var row = sheet.getRow(i);
	if (row.getPhysicalNumberOfCells() == 0) continue;
	if (headers == null) headers = parseRow(row);
	else dataRows.push(parseRow(row));
}

if (wellIdColName == null) wellIdColName = guessWellColumn(headers);
if (wellIdColName == null) throw "Well id column not found";
var wellIdCol = findString(headers, wellIdColName, false);
if (wellIdCol == -1) throw "Well id column not found: " + wellIdColName;

var dims = calculatePlateSize(dataRows.length, 1, true);
var plate = modelUtils.createPlate(model, dims[0], dims[1]);

for (var i=0; i<dataRows.length; i++) {
	var wellId = dataRows[i][wellIdCol];
	var pos = null;
	if (API.get("NumberUtils").isNumeric(wellId)) {
		pos = API.get("NumberUtils").getWellPosition(parseInt(wellId), plate.getColumns());
	} else {
		pos = [ API.get("NumberUtils").convertToRowNumber(wellId), API.get("NumberUtils").convertToColumnNumber(wellId) ];
	}
	var well = plate.getWell(pos[0], pos[1]);
	
	for (var j in headers) {
		var featureValue = API.get("ModelUtils").newFeature(headers[j], well);
		var value = dataRows[i][j];
		if (API.get("NumberUtils").isDouble(value)) featureValue.setNumericValue(parseFloat(value));
		else featureValue.setStringValue(value);		
	}
}

function parseRow(row) {
	var rowData = [];
	for (var i=0; i<row.getLastCellNum(); i++) {
		var cell = row.getCell(i);
		if (cell == null) rowData.push(null);
		else if (cell.getCellType() == 0 || cell.getCellType() == 2) rowData.push(cell.getNumericCellValue());
		else if (cell.getCellType() == 1) rowData.push(cell.getStringCellValue());
		else if (cell.getCellType() == 3) rowData.push(null);
		else rowData.push(null);
	}
	return rowData;
}