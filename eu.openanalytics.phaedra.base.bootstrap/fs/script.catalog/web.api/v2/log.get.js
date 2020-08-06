var logFileLocation = "workspace/.metadata/.log";
var logFileContents = API.get("StreamUtils").readAll(logFileLocation);
response.getWriter().write(new java.lang.String(logFileContents));