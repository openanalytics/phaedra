load("script://web.api/header.js");

var env = Java.type("eu.openanalytics.phaedra.base.environment.Screening").getEnvironment();

var wellId = parseInt(urlParts[0]);
var well = env.getEntityManager().find(Java.type("eu.openanalytics.phaedra.model.plate.vo.Well").class, new java.lang.Long(wellId));
var sws = API.get("SubWellService");

var singleFeature = (urlParts.length > 1);
if (singleFeature) {
	features = [ API.get("ProtocolService").getSubWellFeature(parseInt(urlParts[1])) ];
} else {
	var featureList = API.get("ProtocolUtils").getSubWellFeatures(well);
	var wellList = new java.util.ArrayList();
	wellList.add(well);
	sws.preloadData(wellList, featureList, null);
	features = featureList.toArray();
}

var subwelldata = {};
for (var i in features) {
	var f = features[i];

	// Force additional cache check to workaround issue with SubWellDataCache.isCached
	sws.getData(well, f);

	if (f.isNumeric()) {
		var data = sws.getNumericData(well, f);
	} else {
		var data = sws.getStringData(well, f);
	}
	
	if (data == null) continue;
	subwelldata[f.getName()] = new Array(data.length);
	for (var j in data) subwelldata[f.getName()][j] = data[j];
}

http.replyOk(response, subwelldata);
