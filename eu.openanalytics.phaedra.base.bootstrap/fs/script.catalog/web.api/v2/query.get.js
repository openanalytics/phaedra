load("script://web.api/header.js");

/**
 * E.g. /api/query/plates?protocolClassId=32&approvalStatus=2&approvalDate=between(20180101000000,20181025000000)
 * E.g. /api/query/experiments?protocolClassId=32
 * E.g. /api/query/protocols?name=~cell
 */

var objectType = urlParts[0];
var paramMap = request.getParameterMap();

var QueryModel = Java.type("eu.openanalytics.phaedra.base.search.model.QueryModel");
var QueryFilter = Java.type("eu.openanalytics.phaedra.base.search.model.QueryFilter");
var Operator = Java.type("eu.openanalytics.phaedra.base.search.model.Operator");

var formatDate = function(date) {
    if (date == null) return null;
    return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date);
}

var parseDate = function(dateString) {
    if (dateString == null) return null;
    return new java.text.SimpleDateFormat("yyyyMMddHHmmss").parse(dateString);
}

function findType(typeName) {
    var classes = API.get("SearchService").getSupportedClasses().toArray();
    for (var i=0; i<classes.length; i++) {
        var name = ("" + classes[i]).substring(6);
        if (name.toLowerCase().endsWith(typeName.toLowerCase())) return classes[i];
    }
    return null;
}

function performQuery(typeName, filters, resultMapper) {
    var model = new QueryModel();
    model.setType(findType(typeName));

    var maxResults = 100;
    if (paramMap.get("maxResults") != null) {
        maxResults = parseInt(paramMap.get("maxResults")[0]);
        if (maxResults == -1) model.setMaxResultsSet(false);
        else model.setMaxResults(maxResults);
    }

    for (var i in filters) {
        var f = filters[i];
        var filter = new QueryFilter();
        filter.setType(findType(f.type));
        filter.setColumnName(f.field);
        filter.setOperator(f.operator);
        filter.setValue(f.value);
        model.addQueryFilter(filter);
    }

    var startTime = new Date();
    var results = API.get("SearchService").search(model).toArray();
    var elapsed = new Date() - startTime;
    http.logRequest("Executed query [" + elapsed + "ms] [" + results.length + " results]: " + request.getRequestURI() + "?" + request.getQueryString());

    var resultSet = [];
    for (var i=0; i<results.length; i++) {
        resultSet.push(resultMapper(results[i]));
    }

    return {
        query: request.getQueryString(),
        queryDate: formatDate(new java.util.Date()),
        queryDurationMs: elapsed,
        queryUser: request.getAttribute("username"),
        maxResults: maxResults,
        resultType: typeName,
        resultCount: resultSet.length,
        results: resultSet
    };
}

var excludeFields = [];
if (request.getParameter("excludeFields") != null) {
    excludeFields = request.getParameter("excludeFields");
    excludeFields = excludeFields.substring(1, excludeFields.length() - 1).split(",");
}
function include(name) {
    for (var i in excludeFields) {
        if (excludeFields[i] === name) return false;
    }
    return true;
}

var queryType = objectType.substring(0, objectType.length() - 1);
if (objectType == "protocolclasses") queryType = "protocolclass";
if (objectType == "welldata") queryType = "plate";
if (objectType == "subwelldata") queryType = "well";

var queryFilters = [];
var keys = paramMap.keySet().toArray();
for (var i in keys) {
    var key = keys[i];
    if (key == "token" || key == "maxResults" || key == "valueType" || key == "excludeFields" || key == "featureId") continue;
    
    var operator = Operator.EQUALS;
    var v = paramMap.get(key)[0];

    if (v.startsWith("between(")) {
        operator = Operator.BETWEEN;
        v = v.substring("between(".length, v.length() - 1).split(",");
        v = Java.to(v,"java.lang.Comparable[]");
    } else if (v.startsWith("in(")) {
        operator = Operator.IN;
        v = v.substring("in(".length, v.length() - 1).split(",");
        vList = new java.util.ArrayList();
        for (var i=0; i<v.length; i++) vList.add(v[i]);
        v = vList;
    } else if (v.startsWith("~")) {
        operator = Operator.LIKE;
        v = v.substring(1);
    }

    if ((key == "approvalDate" || key == "validationDate" || key == "calculationDate") && v.length > 0) {
        var dates = v;
        for (var i in dates) dates[i] = parseDate(dates[i]);
        queryFilters.push({
            type: "plate",
            field: key,
            operator: operator,
            value: dates
        });
    } else if (key == "plateId") {
        queryFilters.push({
            type: "plate",
            field: "id",
            operator: operator,
            value: v
        });
    } else if (key == "experimentId") {
        queryFilters.push({
            type: "experiment",
            field: "id",
            operator: operator,
            value: v
        });
    } else if (key == "protocolId") {
        queryFilters.push({
            type: "protocol",
            field: "id",
            operator: operator,
            value: v
        });
    } else if (key == "protocolClassId") {
        queryFilters.push({
            type: "protocolclass",
            field: "id",
            operator: operator,
            value: v
        });
    } else {
        queryFilters.push({
            type: queryType,
            field: key,
            operator: operator,
            value: v
        });
    }
}

var writeOutput = function(outputObject) {
    http.replyOk(response, outputObject);
}

var resultMapper = null;
if (objectType == "subwelldata") {
    var featureIds = request.getParameter("featureId");
    var featureList = new java.util.ArrayList();
    if (featureIds == null) {
        // Do not build feature list now; include all features later
    } else if (featureIds.startsWith("in(")) {
        featureIds = featureIds.substring("in(".length, featureIds.length() - 1).split(",");
        for (var i in featureIds) {
            var swf = API.get("ProtocolService").getSubWellFeature(parseInt(featureIds[i]));
            if (swf != null) featureList.add(swf);
        }
    } else {
        var swf = API.get("ProtocolService").getSubWellFeature(parseInt(featureIds));
        if (swf != null) featureList.add(swf);
    }
    
    var header = "";
    var shouldBatchPreload = (request.getParameter("plateId") != null);
    var platesPreloaded = {};

    writeOutput = function(outputObject) {
        response.setContentType("text/csv");
        response.getWriter().write(header + "\r\n");
        for (var i in outputObject.results) {
            var result = outputObject.results[i];
            for (var j in result) {
                response.getWriter().write(result[j] + "\r\n"); 
            }
        }
    }
    resultMapper = function(well) {
        if (header == "") {
            header = "wellId,subwellIndex,";
            if (featureList.isEmpty()) featureList.addAll(API.get("ProtocolUtils").getSubWellFeatures(well));
            for (var i in featureList.toArray()) {
                var f = featureList.get(i);
                header += f.getId();
                if (i + 1 < featureList.size()) header += ",";
            }
        }
        var sws = API.get("SubWellService");
        var wellList = new java.util.ArrayList();
        if (shouldBatchPreload) {
            var plateId = well.getPlate().getId();
            if (platesPreloaded[plateId] == null) {
                wellList.addAll(well.getPlate().getWells());
                platesPreloaded[plateId] = true;
            }
        } else {
            wellList.add(well);
        }
        sws.preloadData(wellList, featureList, null);
        var formatter = Java.type("eu.openanalytics.phaedra.model.protocol.util.Formatters").getInstance();

        var lines = [];
        var cellCount = sws.getNumberOfCells(well);
        
        var dataPerFeature = {};
        for (var i in featureList.toArray()) {
            var f = featureList.get(i);
            sws.getData(well, f);

            if (f.isNumeric()) {
                dataPerFeature[f.getId()] = sws.getNumericData(well, f);
            } else {
                dataPerFeature[f.getId()] = null;
            }
        }

        for (var c = 0; c < cellCount; c++) {
            var line = "" + well.getId() + "," + c + ",";
            for (var i in featureList.toArray()) {
                var f = featureList.get(i);
                var value = "NaN";
                if (dataPerFeature[f.getId()] != null && dataPerFeature[f.getId()].length > c) {
                    value = formatter.format(dataPerFeature[f.getId()][c], "#.###");
                }
                line += value;
                if (i + 1 < featureList.size()) line += ",";
            }
            lines.push(line);
        }
        return lines;
    }
} else if (objectType == "welldata") {
    var featureIds = request.getParameter("featureId");
    var featureList = new java.util.ArrayList();
    if (featureIds == null) {
        // Do not build feature list now; include all features later
    } else if (featureIds.startsWith("in(")) {
        featureIds = featureIds.substring("in(".length, featureIds.length() - 1).split(",");
        for (var i in featureIds) {
            var f = API.get("ProtocolService").getFeature(parseInt(featureIds[i]));
            if (f != null) featureList.add(f);
        }
    } else {
        var f = API.get("ProtocolService").getFeature(parseInt(featureIds));
        if (f != null) featureList.add(f);
    }
    
    var header = "";

    writeOutput = function(outputObject) {
        response.setContentType("text/csv");
        response.getWriter().write(header + "\r\n");
        for (var i in outputObject.results) {
            var result = outputObject.results[i];
            for (var j in result) {
                response.getWriter().write(result[j] + "\r\n"); 
            }
        }
    }
    resultMapper = function(plate) {
        if (header == "") {
            header = "plateId,wellId,";
            if (featureList.isEmpty()) featureList.addAll(API.get("ProtocolUtils").getFeatures(plate));
            for (var i in featureList.toArray()) {
                var f = featureList.get(i);
                header += f.getId();
                if (i + 1 < featureList.size()) header += ",";
            }
        }

        var valueType = request.getParameter("valueType");
        var accessor = API.get("CalculationService").getAccessor(plate);
        accessor.loadEager(featureList);

        var lines = [];
        var wellCount = plate.getWells().size();
        var formatter = Java.type("eu.openanalytics.phaedra.model.protocol.util.Formatters").getInstance();

        for (var w = 0; w < wellCount; w++) {
            var well = plate.getWells().get(w);
            var line = "" + well.getPlate().getId() + "," + well.getId() + ",";
            for (var i in featureList.toArray()) {
                var f = featureList.get(i);
                if (f.isNumeric()) {
                    var value = accessor.getNumericValue(w+1, f, (valueType == "norm") ? f.getNormalization() : null);
                    value = formatter.format(value, "#.###");
                } else {
                    value = accessor.getStringValue(w+1, f);
                }
                line += value;
                if (i + 1 < featureList.size()) line += ",";
            }
            lines.push(line);
        }
        return lines;
    }
} else if (queryType == "well") {
    resultMapper = function(well) {
        return {
            id: well.getId(),
            plateId: well.getPlate().getId(),
            row: well.getRow(),
            column: well.getColumn(),
            description: well.getDescription(),
            status: well.getStatus(),
            wellType: well.getWellType(),
            compoundId: (well.getCompound() == null) ? null : well.getCompound().getId(),
            compoundType: (well.getCompound() == null) ? null : well.getCompound().getType(),
            compoundNumber: (well.getCompound() == null) ? null : well.getCompound().getNumber(),
            compoundSaltform: (well.getCompound() == null) ? null : well.getCompound().getSaltform(),
            concentration: well.getCompoundConcentration()
        };
    };
} else if (queryType == "plate") {
    resultMapper = function(plate) {
        var plateProps = undefined;
        if (include("properties")) {
            plateProps = [{}];
            var platePropsMap = API.get("PlateService").getPlateProperties(plate);
            for (var i in platePropsMap) plateProps[0][i] = platePropsMap.get(i);
        }

        var wells = undefined;
        if (include("wells")) {
            wells = [];
            var wellObjects = plate.getWells().toArray();
            for (var i in wellObjects) {
                var well = wellObjects[i];
                wells.push({
                    id: well.getId(),
                    row: well.getRow(),
                    column: well.getColumn(),
                    wellNr: API.get("NumberUtils").getWellNr(well.getRow(), well.getColumn(), plate.getColumns()),
                    status: well.getStatus(),
                    description: well.getDescription(),
                    wellType: well.getWellType(),
                    compoundId: (well.getCompound() == null) ? null : well.getCompound().getId(),
                    compoundType: (well.getCompound() == null) ? null : well.getCompound().getType(),
                    compoundNumber: (well.getCompound() == null) ? null : well.getCompound().getNumber(),
                    compoundSaltform: (well.getCompound() == null) ? null : well.getCompound().getSaltform(),
                    concentration: well.getCompoundConcentration()
                });
            }
        }

        return {
            id: plate.getId(),
            experimentId: plate.getExperiment().getId(),
            protocolId: plate.getExperiment().getProtocol().getId(),
            protocolClassId: plate.getExperiment().getProtocol().getProtocolClass().getId(),
            barcode: plate.getBarcode(),
            rows: plate.getRows(),
            columns: plate.getColumns(),
            sequence: plate.getSequence(),
            info: plate.getInfo(),
            description: plate.getDescription(),
            properties: plateProps,
            calculationStatus: plate.getCalculationStatus(),
            calculationDate: formatDate(plate.getCalculationDate()),
            calculationError: plate.getCalculationError(),
            validationStatus: plate.getValidationStatus(),
            validationDate: formatDate(plate.getValidationDate()),
            validationUser: plate.getValidationUser(),
            approvalStatus: plate.getApprovalStatus(),
            approvalDate: formatDate(plate.getApprovalDate()),
            approvalUser: plate.getApprovalUser(),
            uploadStatus: plate.getUploadStatus(),
            uploadDate: plate.getUploadDate(),
            uploadUser: plate.getUploadUser(),
            wells: wells
        };
    };
} else if (queryType == "experiment") {
    resultMapper = function(exp) {
        return {
            id: exp.getId(),
            protocolId: exp.getProtocol().getId(),
            protocolClassId: exp.getProtocol().getProtocolClass().getId(),
            name: exp.getName(),
            date: formatDate(exp.getCreateDate()),
            creator: exp.getCreator(),
            description: exp.getDescription()
        };
    };
} else if (queryType == "protocol") {
    resultMapper = function(protocol) {
        return {
            id: protocol.getId(),
            protocolClassId: protocol.getProtocolClass().getId(),
            name: protocol.getName(),
            description: protocol.getDescription(),
            uploadSystem: protocol.getUploadSystem(),
            team: protocol.getTeamCode()
        };
    };
} else if (queryType == "protocolclass") {
    resultMapper = function(pclass) {
        var featureArr = undefined;
        if (include("features")) {
            featureArr = [];
            var features = pclass.getFeatures().toArray();
            for (var i in features) {
                var f = features[i];
                featureArr.push({
                    "id": f.getId(),
                    "name": f.getName(),
                    "alias": f.getShortName(),
                    "key": f.isKey(),
                    "upload": f.isUploaded(),
                    "normalization": f.getNormalization()
                });
            }
        }

        var swFeatureArr = undefined;
        if (include("subWellFeatures")) {
            swFeatureArr = [];
            var swFeatures = pclass.getSubWellFeatures().toArray();
            for (var i in swFeatures) {
                var f = swFeatures[i];
                swFeatureArr.push({
                    "id": f.getId(),
                    "name": f.getName(),
                    "alias": f.getShortName(),
                    "key": f.isKey()
                });
            }
        }

        var imageSettings = undefined;
        if (include("imageSettings")) {
            var channelArr = [];
            var channels = pclass.getImageSettings().getImageChannels().toArray();
            for (var i in channels) {
                var c = channels[i];
                channelArr.push({
                    "id": c.getId(),
                    "name": c.getName(),
                    "sequence": c.getSequence(),
                    "depth": c.getBitDepth(),
                    "colorMask": c.getColorMask(),
                    "levelMin": c.getLevelMin(),
                    "levelMax": c.getLevelMax()
                });
            }
            imageSettings = {
		        id: pclass.getImageSettings().getId(),
		        gamma: pclass.getImageSettings().getGamma(),
		        channels: channelArr
	        };
        }

        return {
            id: pclass.getId(),
            name: pclass.getName(),
            description: pclass.getDescription(),
            features: featureArr,
            defaultFeature: include("defaultFeature") ? pclass.getDefaultFeature() : undefined,
	        subWellFeatures: swFeatureArr,
	        imageSettings: imageSettings
        };
    };
}

var output = performQuery(queryType, queryFilters, resultMapper);
writeOutput(output);