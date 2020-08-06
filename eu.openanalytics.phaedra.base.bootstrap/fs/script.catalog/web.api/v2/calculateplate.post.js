load("script://web.api/header.js");

var plateId = parseInt(urlParts[0]);
var plate = API.get("PlateService").getPlateById(plateId);

var CalcModes = Java.type("eu.openanalytics.phaedra.calculation.CalculationService.CalculationMode");
var calcMode = CalcModes.NORMAL;
if (urlParts.length > 1) calcMode = CalcModes.valueOf(urlParts[1].toUpperCase());

API.get("CalculationService").calculate(plate, calcMode);
http.replyOk(response, { "status": "ok" });