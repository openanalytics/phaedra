load("script://web.api/header.js");

if (!Java.type("eu.openanalytics.phaedra.base.security.SecurityService").getInstance().isGlobalAdmin(request.getAttribute("username"))) {
    http.replyError(response, 401, "Not authorized to perform data writes via HTTP")
} else {
    var plateId = parseInt(urlParts[0]);
    var featureId = parseInt(urlParts[1]);
    
    var plate = API.get("PlateService").getPlateById(plateId);
    var feature = API.get("ProtocolService").getFeature(featureId);
    
    var data = JSON.parse(http.readBodyAsString(request));
    for (var i in data) {
        if (data[i] == null) data[i] = "NaN"
    }
    
    API.get("PlateService").updateWellDataRaw(plate, feature, Java.to(data, "double[]"));
    API.get("CalculationService").getAccessor(plate).reset(true);
}
