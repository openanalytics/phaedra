load("script://web.api/header.js");

var wellId = parseInt(urlParts[0]);
var featureId = parseInt(urlParts[1]);

var env = Java.type("eu.openanalytics.phaedra.base.environment.Screening").getEnvironment();
var well = env.getEntityManager().find(Java.type("eu.openanalytics.phaedra.model.plate.vo.Well").class, new java.lang.Long(wellId));
var feature = API.get("ProtocolService").getSubWellFeature(featureId);

// Extra safety: can only update sw data if you own the experiment, or are admin
//TODO experiments imported by the DC server are owned by service account
var username = request.getAttribute("username");
var isAdmin = Java.type("eu.openanalytics.phaedra.base.security.SecurityService").getInstance().isGlobalAdmin(username);
var isExpOwner = username.equalsIgnoreCase(well.getPlate().getExperiment().getCreator());

if (!isAdmin && !isExpOwner) {
    http.replyError(response, 401, "Not authorized to modify plate " + well.getPlate());
} else {
    var data = JSON.parse(http.readBodyAsString(request));
    for (var i in data) {
        if (data[i] == null) data[i] = "NaN"
    }

    var dataMap = new java.util.HashMap();
    dataMap.put(well, Java.to(data, "float[]"));

    API.get("SubWellService").updateData(dataMap, feature);
    API.get("SubWellService").removeFromCache(well, feature);
}
