load("script://web.api/header.js");

/*
Examples:

/api/image/plate/119
/api/image/plate/119?size=40

/api/image/well/43322
/api/image/well/43322?cell=102
/api/image/well/43322?scale=0.25
/api/image/well/43322?region=200,200,300,300

/api/image/well/43322?components=0,1,1,1
/api/image/well/43322?contrast=4000,4000,2000
/api/image/well/43322?format=jpeg

*/

var object = urlParts[0];
var id = parseInt(urlParts[1]);
var queryArgs = http.getQueryArgs(request);

function generateImageRequest(well) {
	var components = null;
	if (queryArgs.components) {
		components = [];
		var list = queryArgs.components.split(",");
		for (i in list) components[i] = (list[i] === "1");
	}

	var customSettings = null;
	if (queryArgs.contrast) {
		var ImageSettings = Java.type("eu.openanalytics.phaedra.model.protocol.vo.ImageSettings");
		customSettings = new ImageSettings();
		customSettings.setImageChannels([]);

		var pClass = API.get("ProtocolUtils").getProtocolClass(well);
		originalSettings = pClass.getImageSettings();
		var copier = Java.type("eu.openanalytics.phaedra.model.protocol.util.ObjectCopyFactory");
		copier.copySettings(originalSettings, customSettings, false);

		contrastArgs = queryArgs.contrast.split(",");
		for (var i in contrastArgs) {
			if (contrastArgs[i].isEmpty()) continue;
			ch = customSettings.getImageChannels().get(i);
			minMax = contrastArgs[i].split("-");
			if (minMax.length == 1) {
				ch.setLevelMax(parseInt(minMax[0]));
			} else {
				ch.setLevelMin(parseInt(minMax[0]));
				ch.setLevelMax(parseInt(minMax[1]));
			}
		}
	}

	var region = null;
	if (queryArgs.region) {
		var regionArgs = queryArgs.region.split(",");
		region = new org.eclipse.swt.graphics.Rectangle(regionArgs[0], regionArgs[1], regionArgs[2], regionArgs[3]);
	}

	var imgReqFactory = Java.type("eu.openanalytics.phaedra.wellimage.render.ImageRenderRequestFactory");
	if (queryArgs.cell) {
	    imgReq = imgReqFactory.forSubWell(well, queryArgs.cell);
	} else {
	    imgReq = imgReqFactory.forWell(well).withRegion(region);
	}
	return imgReq
	    .withScale(queryArgs.scale || 1.0)
	    .withComponents(components)
		.withCustomSettings(customSettings)
	    .build();
}

function encodeImage(img) {
	var formatTable = {
		bmp: 0,
		jpg: 4,
		jpeg: 4,
		png: 5
	};
	var format = queryArgs.format || "png";

	var loader = new org.eclipse.swt.graphics.ImageLoader();
	loader.data = [ image ];
	var out = new java.io.ByteArrayOutputStream();
	loader.save(out, formatTable[format]);
	return out.toByteArray();
}

loadScript("image/" + object + ".js");
imageBytes = encodeImage(image);

response.setContentType("image/" + (queryArgs.format || "png"));
response.getOutputStream().write(imageBytes);
response.getOutputStream().close();
