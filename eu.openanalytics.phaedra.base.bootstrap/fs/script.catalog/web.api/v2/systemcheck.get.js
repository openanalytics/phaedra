response.getWriter().write("<html><head></head><body><table>");

printTableRow("Server", "UP", "color: green");

var startTime = java.lang.Long.parseLong(java.lang.System.getProperty("eclipse.startTime"));
var uptime = java.lang.System.currentTimeMillis() - startTime;
var uptimeMinutes = uptime/60000;
var daysUp = Math.floor(uptimeMinutes/(60*24));
var hoursUp = Math.floor((uptimeMinutes/60) - (daysUp*24));
var minutesUp = Math.floor(uptimeMinutes - (daysUp*24*60 + hoursUp*60));

printTableRow("Uptime", daysUp + "d " + hoursUp + "h " + minutesUp + "m");

printTableRow("Operating System", java.lang.System.getProperty("os.name"));
printTableRow("Architecture", java.lang.System.getProperty("os.arch"));
printTableRow("Java Version", java.lang.System.getProperty("java.version"));

var memMax = java.lang.Runtime.getRuntime().maxMemory();
var memTotal = java.lang.Runtime.getRuntime().totalMemory(); 
var memFree = java.lang.Runtime.getRuntime().freeMemory();
var memUsed = memTotal - memFree;

printTableRow("Java Heap", Math.floor(memUsed/1024/1024) + "MB / " + Math.floor(memMax/1024/1024) + " MB used");

var env = Java.type("eu.openanalytics.phaedra.base.environment.Screening").getEnvironment();

printTableRow("Phaedra Environment", env.getName());

try {
	printTableRow("Phaedra File Server", env.getFileServer().getAsFile("").getAbsolutePath());

	var fsRoot = new java.io.File(env.getFileServer().getAsFile("").getAbsolutePath());
	var free = parseInt(fsRoot.getFreeSpace() / (1024*1024*1024));
	var total = parseInt(fsRoot.getTotalSpace() / (1024*1024*1024));
	var pctUsed = 100 - (parseInt((free/total)*10000))/100;

	printTableRow("", (total-free) + " / " + total + " GB used (" + pctUsed + "% full)");
} catch (err) {
	printTableRow("Phaedra File Server", "S3");
}

var conn = env.getJDBCConnection();
var dbUrl = conn.getMetaData().getURL();
conn.close();
printTableRow("Phaedra Database", dbUrl);

response.getWriter().write("</table></body></html>");

function printTableRow(header, value, style) {
	if (style === undefined) response.getWriter().write("<tr><td><b>" + header + "</b></td><td>" + value + "</td></tr>");
	else response.getWriter().write("<tr><td><b>" + header + "</b></td><td style='" + style + "'>" + value + "</td></tr>");
}