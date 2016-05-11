load("script://dc/common.js");

API.get("ModelUtils").newPlate(model);
var doc = API.get("XmlUtils").parse(new java.lang.String(data));

forEachTag(doc, "/AnalysisResults/Areas/Area/Wells/Well", function (tag) {
	var row = parseInt(tag.getAttribute("row"));
	var col = parseInt(tag.getAttribute("col"));
	
	var plate = model.getPlate(0);
	var well =  API.get("ModelUtils").newWell(plate, row, col);
	if (row > plate.getRows()) plate.setRows(row);
	if (col > plate.getColumns()) plate.setColumns(col);
	
	var resultTags = tag.getElementsByTagName("Result");
	for (var i=0; i<resultTags.getLength(); i++) {
		var resultTag = resultTags.item(i);
		var featureName = resultTag.getAttribute("name");
		var value = API.get("XmlUtils").getNodeValue(resultTag);
		
		var featureValue = API.get("ModelUtils").newFeature(featureName, well);
		if (API.get("NumberUtils").isDouble(value)) featureValue.setNumericValue(parseFloat(value));
		else featureValue.setStringValue(value);
	}
});