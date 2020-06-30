// API.get("ScriptService").getCatalog().run("misc/generateCalcFeatureByTemplate", [618, "CellCount", "median.polish.template.json"])

var pClassId = parseInt(args[0]);
var baseFeatureName = args[1];

var templateData = API.get("ScriptService").getFeatureTemplateCatalog().getFeatureTemplate(args[2]);
var features = JSON.parse(templateData);

for (var i in features) {
	var feature = {"pClassId": pClassId, "baseFeatureName": baseFeatureName, "featureConf": features[i]};

	API.get("ScriptService").getCatalog().run("misc/generateCalcFeature", feature);
}