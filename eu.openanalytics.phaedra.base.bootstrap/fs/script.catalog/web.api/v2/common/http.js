http = {};

http.replyOk = function(response, object) {
  response.setContentType("application/json");
  response.getWriter().write(JSON.stringify(object, null, 2));
};

http.replyError = function(response, status, msg) {
  response.setStatus(status);
  response.getWriter().write(msg);
  ScriptHandledException.throwNew();
};

http.readBodyAsString = function(request) {
  input = request.getInputStream();
  bytes = Java.type("eu.openanalytics.phaedra.base.util.io.StreamUtils").readAll(input);
  return new java.lang.String(bytes);
};

http.getQueryArgs = function(request) {
    var queryArgs = {}

    var queryString = request.getQueryString();
    if (queryString == null) return queryArgs;
    
    var queryArgsArray = queryString.split("&");
    for (var i in queryArgsArray) {
    	arg = queryArgsArray[i].split("=");
    	queryArgs[arg[0].toLowerCase()] = arg[1];
    }
    return queryArgs;
};

http.logRequest = function(msg) {
  Java.type("eu.openanalytics.phaedra.base.util.misc.EclipseLog").info(msg, "eu.openanalytics.phaedra.http.api");
}