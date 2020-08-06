load("script://web.api/header.js");

var compoundId = parseInt(urlParts[0]);
var featureId = parseInt(urlParts[1]);

var env = Java.type("eu.openanalytics.phaedra.base.environment.Screening").getEnvironment();
var compound = env.getEntityManager().find(Java.type("eu.openanalytics.phaedra.model.plate.vo.Compound").class, new java.lang.Long(compoundId));
var feature = API.get("ProtocolService").getFeature(featureId);

var curve = API.get("CurveFitService").getCurve(compound.getWells().get(0), feature);

if (curve != null) {
	var map = {
		"id": curve.getId(),
		"featureId": feature.getId(),
		"model": curve.getModelId(),
		"fitDate": curve.getFitDate(),
		"fitVersion": curve.getFitVersion(),
		"errorCode": curve.getErrorCode(),
	};
	
	for (var i in curve.getOutputParameters()) {
		var param = curve.getOutputParameters()[i];
		map[param.definition.name] = Java.type("eu.openanalytics.phaedra.model.curve.CurveParameter").renderValue(param, curve, null);
	}
	
	http.replyOk(response, map);
}