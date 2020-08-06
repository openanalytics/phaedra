load("script://web.api/" + apiVersion + "/common/http.js");
load("script://web.api/" + apiVersion + "/common/jdbc.js");

var checkAuth = (request.getServerName() != "localhost");

//TODO Re-enable auth for these calls when client auth header is implemented
var reqPath = request.getPathInfo();
if (reqPath.contains("/compinfo") || reqPath.contains("/platedef")) {
    checkAuth = false;
}
if (checkAuth) security.checkToken(request);

var loadScript = function(name) {
    load("script://web.api/" + apiVersion + "/" + name);
}

var logger = Java.type("eu.openanalytics.phaedra.base.util.misc.EclipseLog");
logger.debug("API call: " + request.getMethod() + " " + reqPath, API.get("ScriptService").getClass());