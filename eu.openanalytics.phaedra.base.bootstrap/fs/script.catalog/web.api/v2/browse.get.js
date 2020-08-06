load("script://web.api/header.js");

var objectType = urlParts[0];

var plateService = API.get("PlateService");
var protocolService = API.get("ProtocolService");

var getItems = function(allResolver, idResolver, nameResolver) {
	if (urlParts.length == 1) {
		return allResolver();
	} else if (urlParts.length > 1) {
		var arg = urlParts[1];
		if (arg.contains(",")) {
			var ids = arg.split(",");
			var mapped = [];
			for (var i in ids) {
				mapped.push(idResolver(parseInt(ids[i])));
			}
			return mapped;
		} else if (parseInt(arg) > 0) {
			return [ idResolver(parseInt(arg)) ];
		} else {
			return nameResolver(arg);
		}
	}
	return null;
}

var formatDate = function(date) {
	if (date == null) return null;
	return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date);
}

var output = [];
if (objectType == "protocols") {
	// Query 1 or multiple protocols
	var items = getItems(
		function() { return protocolService.getProtocols().toArray(); },
		function(id) {
			var match = protocolService.getProtocol(id);
			if (match == null) http.replyError(response, 404, "No protocol found with id " + id);
			return match;
		},
		function(like) {
			var arr = [];
			var allProtocols = protocolService.getProtocols().toArray();
			for (var i in allProtocols) arr.push(allProtocols[i]);
			return arr.filter(function(p) { return p.getName().toLowerCase().contains(like); });
		}
	);
	for (var i=0; i<items.length; i++) {
		var item = new java.util.HashMap();
		item.put("id", items[i].getId());
		item.put("name", items[i].getName());
		item.put("description", items[i].getDescription());
		item.put("uploadSystem", items[i].getUploadSystem());
		item.put("protocolClassId", items[i].getProtocolClass().getId());
		item.put("team", items[i].getTeamCode());
		output.push(item);
	}
} else if (objectType == "protocol") {
	// Query experiments for 1 protocol
	var objectId = parseInt(urlParts[1]);
	var protocol = protocolService.getProtocol(objectId);
	if (protocol == null) http.replyError(response, 404, "No protocol found with id " + objectId);
	var items = plateService.getExperiments(protocol).toArray();
	for (var i=0; i<items.length; i++) {
		var item = new java.util.HashMap();
		item.put("id", items[i].getId());
		item.put("name", items[i].getName());
		item.put("date", items[i].getCreateDate());
		item.put("creator", items[i].getCreator());
		item.put("description", items[i].getDescription());
		output.push(item);
	}
} else if (objectType == "experiments") {
	// Query 1 or multiple experiments
	var getAllExperiments = function() {
		var protocols = protocolService.getProtocols().toArray();
		var allExperiments = [];
		for (var i in protocols) {
			var experiments = plateService.getExperiments(protocols[i]).toArray();
			for (var j in experiments) allExperiments.push(experiments[j]);
		}
		return allExperiments;
	}
	var items = getItems(
		getAllExperiments,
		function(id) {
			var match = plateService.getExperiment(id);
			if (match == null) http.replyError(response, 404, "No experiment found with id " + id);
			return match;
		},
		function(like) {
			var arr = [];
			var allExperiments = getAllExperiments();
			for (var i in allExperiments) arr.push(allExperiments[i]);
			return arr.filter(function(p) { return p.getName().toLowerCase().contains(like); });
		}
	);
	for (var i=0; i<items.length; i++) {
		var item = new java.util.HashMap();
		item.put("id", items[i].getId());
		item.put("name", items[i].getName());
		item.put("date", items[i].getCreateDate());
		item.put("creator", items[i].getCreator());
		item.put("description", items[i].getDescription());
		output.push(item);
	}
} else if (objectType == "experiment") {
	// Query plates for 1 experiment
	var objectId = parseInt(urlParts[1]);
	var experiment = plateService.getExperiment(objectId);
	if (experiment == null) http.replyError(response, 404, "No experiment found with id " + objectId);
	var items = plateService["getPlates(eu.openanalytics.phaedra.model.plate.vo.Experiment)"](experiment).toArray();
	for (var i=0; i<items.length; i++) {
		var item = new java.util.HashMap();
		item.put("id", items[i].getId());
		item.put("rows", items[i].getRows());
		item.put("columns", items[i].getColumns());
		item.put("barcode", items[i].getBarcode());
		item.put("sequence", items[i].getSequence());
		item.put("description", items[i].getDescription());
		output.push(item);
	}
} else if (objectType == "plates") {
	// Query 1 or multiple plates
	var getAllPlates = function() {
		var pClasses = protocolService.getProtocolClasses().toArray();
		var allPlates = [];
		for (var i in pClasses) {
			var plates = plateService.getPlates("", pClasses[i]).toArray();
			for (var j in plates) allPlates.push(plates[j]);
		}
		return allPlates;
	}
	var items = getItems(
		getAllPlates,
		function(id) {
			var match = plateService.getPlateById(id);
			if (match == null) http.replyError(response, 404, "No plate found with id " + id);
		},
		function(like) {
			var arr = [];
			var allPlates = getAllPlates();
			for (var i in allPlates) arr.push(allPlates[i]);
			return arr.filter(function(p) { return p.getBarcode().toLowerCase().contains(like); });
		}
	);
	for (var i=0; i<items.length; i++) {
		var item = new java.util.HashMap();
		item.put("id", items[i].getId());
		item.put("rows", items[i].getRows());
		item.put("columns", items[i].getColumns());
		item.put("barcode", items[i].getBarcode());
		item.put("sequence", items[i].getSequence());
		item.put("description", items[i].getDescription());
		item.put("approvalStatus", items[i].getApprovalStatus());
		item.put("approvalDate", formatDate(items[i].getApprovalDate()));
		output.push(item);
	}
}

// http.replyOk(response, output);

var json = json.toJson(Java.to(output,"java.lang.Object[]"), null);
response.setContentType("application/json");
response.getWriter().write(json);
