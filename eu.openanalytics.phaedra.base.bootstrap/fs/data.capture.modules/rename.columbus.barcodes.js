load("script://dc/common.js");

var regex = "(.*)--Me(\\d+)";
var pattern = java.util.regex.Pattern.compile(regex);

forEachReading(function(reading) {
	var matcher = pattern.matcher(reading.getBarcode());
	if (matcher.matches()) {
		var newBC = matcher.group(1);
		var store = ctx.getStore(reading);
		if (API.get("CaptureUtils").resolveVars("${barcode.append.meas.id}", false) == "true", ctx) {
			newBC += " " + parseInt(store.getProperty("/", "MeasId"));
		}
		if (API.get("CaptureUtils").resolveVars("${barcode.append.meas.date}", false) == "true", ctx) {
			newBC += " " + store.getProperty("/", "MeasDate").toString();
		}
		reading.setBarcode(newBC);
		store.setProperty("/", "MeasId", matcher.group(2));
	}
});