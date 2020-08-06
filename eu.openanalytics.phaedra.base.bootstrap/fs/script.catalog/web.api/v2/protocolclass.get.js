load("script://web.api/header.js");

var id = parseInt(urlParts[0]);
var pc = API.get("ProtocolService").getProtocolClass(id);

var map = {
	"id": pc.getId(),
	"name": pc.getName(),
	"description": pc.getDescription(),
	"features": [],
	"defaultFeature": (pc.getDefaultFeature() == null) ? null : pc.getDefaultFeature().getId(),
	"subWellFeatures": [],
	"imageSettings": {
		"id": pc.getImageSettings().getId(),
		"gamma": pc.getImageSettings().getGamma(),
		"channels": []
	}
};

var features = pc.getFeatures().toArray();
for (var i in features) {
	var f = features[i];
	var featureMap = {
		"id": f.getId(),
		"name": f.getName(),
		"alias": f.getShortName(),
		"key": f.isKey(),
		"upload": f.isUploaded(),
		"normalization": f.getNormalization()
	};
	map["features"].push(featureMap);
}

features = pc.getSubWellFeatures().toArray();
for (var i in features) {
	var f = features[i];
	var featureMap = {
		"id": f.getId(),
		"name": f.getName(),
		"alias": f.getShortName(),
		"key": f.isKey()
	};
	map["subWellFeatures"].push(featureMap);
}

var channels = pc.getImageSettings().getImageChannels().toArray();
for (var i in channels) {
	var c = channels[i];
	var channelMap = {
		"id": c.getId(),
		"name": c.getName(),
		"sequence": c.getSequence(),
		"depth": c.getBitDepth(),
		"colorMask": c.getColorMask(),
		"levelMin": c.getLevelMin(),
		"levelMax": c.getLevelMax()
	};
	map["imageSettings"]["channels"].push(channelMap);
}

response.setContentType("application/json");
response.getWriter().write(JSON.stringify(map));