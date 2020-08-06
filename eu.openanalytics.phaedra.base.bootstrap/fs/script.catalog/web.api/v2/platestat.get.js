load("script://web.api/header.js");

var plateId = parseInt(urlParts[0]);
var featureId = parseInt(urlParts[1]);
var stat = urlParts[2];
var wellType = (urlParts.length > 3) ? urlParts[3] : null;

var plate = API.get("PlateService").getPlateById(plateId);
var feature = API.get("ProtocolService").getFeature(featureId);
var retVal = API.get("StatService").calculate(stat, plate, feature, wellType, null);

response.setContentType("application/json");
response.getWriter().write(json.toJson(retVal, null));