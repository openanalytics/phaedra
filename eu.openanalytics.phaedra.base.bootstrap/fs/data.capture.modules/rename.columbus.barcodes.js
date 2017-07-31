load("script://dc/common.js");

var regex = "(.*)--Me(\\d+)";
var pattern = java.util.regex.Pattern.compile(regex);

forEachReading(function(reading) {
	var matcher = pattern.matcher(reading.getBarcode());
	if (matcher.matches()) {
		var newBC = matcher.group(1);
		var store = ctx.getStore(reading);
		if (resolveVars("${barcode.append.meas.id}") == "true") newBC += " " + parseInt(store.getProperty("/", "MeasId"));
		if (resolveVars("${barcode.append.meas.date}") == "true") newBC += " " + store.getProperty("/", "MeasDate").toString();
		reading.setBarcode(newBC);
		store.setProperty("/", "MeasId", matcher.group(2));
	}
});