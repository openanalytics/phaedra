load("script://dc/common.js");

var filePath = getParameter("file.path", ".");
var filePattern = getParameter("file.pattern");
var parserId = getParameter("parser.id");
var isOptional = getParameter("optional", false);

// optionally automatically reject wells during data capture
var autoRejectMissingEnabled = getParameter("auto.reject.missing", false); // true to enable auto reject of empty wells
var autoRejectStatusColumn = getParameter("autoRejectStatusColumn"); // the name of column with status values used for auto reject
var autoRejectStatusValue = getParameter("autoRejectStatusValue", 0); // the status value triggering auto reject, default 0
var wellIdColumn = getParameter("wellid.column");

forEachReading(function(reading) {
	// Locate the file.
	var dataFile = findFile(filePath, filePattern, isOptional);
	reading.setFileName(API.get("FileUtils").getName(dataFile));

	// Parse the file.
	var model = parseFile(dataFile, parserId);
	
	// Auto reject by status
	if (autoRejectStatusColumn != null) {
		var plates = model.getPlates();
		var rejectStatusFeatureId = findFeatureId(plates[0], autoRejectStatusColumn);
		if (rejectStatusFeatureId == null) throw "Auto reject column not found";
		rejectStatusValue = Number(autoRejectStatusValue);
		
		for (var iPlate in plates) {
			var wells = plates[iPlate].getWells();
			for (var iWell in wells) {
				var featureValue = wells[iWell].getFeature(rejectStatusFeatureId);
				if (featureValue != null && featureValue.getNumericValue() == rejectStatusValue) {
					wells[iWell].setRejected(true);
				}
			}
		}
	}
	if (autoRejectMissingEnabled) {
		var plates = model.getPlates();
		var rejectIgnoreFeatureIds = [];
		function isEmpty(well) {
			var ids = well.getFeatureIds();
			for (var iId in ids) {
				if (rejectIgnoreFeatureIds.indexOf(ids[iId]) >= 0) continue;
				var featureValue = well.getFeature(ids[iId]);
				var v;
				if (featureValue != null && (
						((v = featureValue.getStringValue()) != null && v.length > 0)
						|| ((v = featureValue.getNumericValue()) != null && !(isNaN(v) || v == 0)) )) {
					return false;
				}
			}
			return true;
		}
		function addIgnore(id) {
			if (id != null) {
				rejectIgnoreFeatureIds.push(id);
			}
		}
		if (wellIdColumn != null) {
			addIgnore(findFeatureId(plates[0], wellIdColumn));
		}
		else {
			var column = guessWellColumn(plates[0].getWells()[0].getFeatureIds());
			if (column != null) {
				addIgnore(findFeatureId(plates[0], column));
			}
		}
		
		for (var iPlate in plates) {
			var wells = plates[iPlate].getWells();
			for (var iWell in wells) {
				if (isEmpty(wells[iWell])) {
					wells[iWell].setRejected(true);
				}
			}
		}
	}
	
	// Save the parsed model.
	// Use the plate dimensions given by the data file.
	saveModel(model);
	reading.setRows(model.getPlate(0).getRows());
	reading.setColumns(model.getPlate(0).getColumns());
});

function findFeatureId(plate, name) {
	var wells = plate.getWells();
	for (var j in wells) {
		var ids = wells[j].getFeatureIds();
		var idx = findString(ids, name, false);
		if (idx >= 0) {
			return ids[idx];
		}
	}
	return null;
}
