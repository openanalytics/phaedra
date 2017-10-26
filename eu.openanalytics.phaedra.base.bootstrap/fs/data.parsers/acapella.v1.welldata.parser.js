load("script://dc/common.js");

var parametersXPath = "/AnalysisResults/ParameterAnnotations/Parameter";
var parameterNameAttr = "name";
var parameterIdAttr = "id";

var resultsXPath = "/AnalysisResults/Results";
var resultsRowAttr= "Row";
var resultsColAttr= "Col";
var resultTagName = "Result";
var resultParAttr = "parID";
var valueTagName = "value";
var valueKindAttr = "kind";

var plate = API.get("ModelUtils").newPlate(model);
var doc = API.get("XmlUtils").parse(new java.lang.String(data));

var parameters = {};
forEachTag(doc, parametersXPath, function(tag, i) {
	var id = tag.getAttribute(parameterIdAttr);
	var name = tag.getAttribute(parameterNameAttr);
	parameters[id] = name;
});

forEachTag(doc, resultsXPath, function(tag) {
	var row = parseInt(tag.getAttribute(resultsRowAttr));
	var col = parseInt(tag.getAttribute(resultsColAttr));
	var well = API.get("ModelUtils").newWell(plate, row, col);
	
	if (row > plate.getRows()) plate.setRows(row);
	if (col > plate.getColumns()) plate.setColumns(col);
	
	var resultTags = tag.getElementsByTagName(resultTagName);
	for (var i=0; i<resultTags.getLength(); i++) {
		var resultTag = resultTags.item(i);
		var valueTag = API.get("XmlUtils").getNodeByName(resultTag, valueTagName);
		var valueKind = valueTag.getAttribute(valueKindAttr);
		var value = API.get("XmlUtils").getNodeValue(valueTag);
			
		var paramId = resultTag.getAttribute(resultParAttr);
		var featureName = parameters[paramId] + " - " + valueKind;
		
		var featureValue = API.get("ModelUtils").newFeature(featureName, well);
		if (API.get("NumberUtils").isDouble(value)) featureValue.setNumericValue(parseFloat(value));
		else featureValue.setStringValue(value);
	}
});

var plateDims = calculatePlateSize(plate.getRows()*plate.getColumns(), 1, true);
plate.setRows(plateDims[0]);
plate.setColumns(plateDims[1]);