load("script://web.api/header.js");

var compoundId = parseInt(urlParts[0]);
var featureId = parseInt(urlParts[1]);

var format = "pdf"
if (urlParts.length > 2) format = urlParts[2];

var env = Java.type("eu.openanalytics.phaedra.base.environment.Screening").getEnvironment();
var compound = env.getEntityManager().find(Java.type("eu.openanalytics.phaedra.model.plate.vo.Compound").class, new java.lang.Long(compoundId));
var feature = API.get("ProtocolService").getFeature(featureId);

var curve = API.get("CurveFitService").getCurve(compound.getWells().get(0), feature);

if (curve != null) {
	var plot = curve.getPlot();
	if (format == "svg") {
		converter = Java.type("eu.openanalytics.phaedra.base.util.convert.PDFToImageConverter");
		plot = converter.convertToSVG(plot, 200, 200);
	}

	response.setContentType("application/pdf");
	response.getOutputStream().write(plot);
}