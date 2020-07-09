// API.get("ScriptService").getCatalog().run("misc/generateCalcFeatures", ["SD", 46])

// Input parameters
var type = args[0]; // "SD" || "DR"
var pClassId = parseInt(args[1]);

var formulaIds = {
    rowEffect: 6,
    colEffect: 7,
    polish: 8,
    merge: 9
};

var config = {
    "SD": {
        features: [
            { suffix: "ColEffect", calcFormula: "#%s#", normFormula: "formula#" + formulaIds.colEffect },
            { suffix: "RowEffect", calcFormula: "#%s#", normFormula: "formula#" + formulaIds.rowEffect },
            { suffix: "Polished", calcFormula: "#%s#", normFormula: "formula#" + formulaIds.polish },
            { suffix: "Merged", calcFormulaId: formulaIds.merge, calcSequence: 1 },
            { suffix: "Fold median", calcFormula: "#%s Merged#", calcSequence: 2, normName: "Fold" },
            { suffix: "PIN neg mean", calcFormula: "#%s Merged#", calcSequence: 2, normName: "PIN[Neg Mean]" },
            { suffix: "PIN neg median", calcFormula: "#%s Merged#", calcSequence: 2, normName: "PIN[Neg Median]" },
            { suffix: "PIN pos mean", calcFormula: "#%s Merged#", calcSequence: 2, normName: "PIN[Pos Mean]" },
            { suffix: "PIN pos median", calcFormula: "#%s Merged#", calcSequence: 2, normName: "PIN[Pos Median]" },
            { suffix: "Z-score negatives", calcFormula: "#%s Merged#", calcSequence: 2, normName: "ZScore[L]" },
            { suffix: "Z-score samples and negatives", calcFormula: "#%s Merged#", calcSequence: 2, normName: "ZScore[S/L]" },
            { suffix: "Rob Z-score negatives", calcFormula: "#%s Merged#", calcSequence: 2, normName: "ZScoreRob[L]" },
            { suffix: "Rob Z-score samples and negatives", calcFormula: "#%s Merged#", calcSequence: 2, normName: "ZScoreRob[S/L]" },
        ]
    },
    "DR": {
        features: [
            { suffix: "Fold median", calcFormula: "#%s#", normName: "Fold" },
            { suffix: "PIN neg mean", calcFormula: "#%s#", normName: "PIN[Neg Mean]", curveModel: "PL4", curveMethod: "OLS", curveType: "A" },
            { suffix: "PIN neg median", calcFormula: "#%s#", normName: "PIN[Neg Median]" },
            { suffix: "PIN pos mean", calcFormula: "#%s#", normName: "PIN[Pos Mean]", curveModel: "PL4", curveMethod: "OLS", curveType: "D" },
            { suffix: "PIN pos median", calcFormula: "#%s#", normName: "PIN[Pos Median]" },
        ]
    }
};

var pClass = API.get("ProtocolService").getProtocolClass(pClassId);
if (pClass == null) throw "Protocol Class not found for ID: " + pClassId;
console.print("Checking features for protocol class " + pClass + ", type " + type + "...");

var existingFeatures = pClass.getFeatures().toArray();
var cfg = config[type].features;

var featuresAdded = false;

for (var i in existingFeatures) {
    var baseFeature = existingFeatures[i];
    var isBaseFeature = true;
    for (var j in cfg) {
        if (baseFeature.getName().endsWith(cfg[j].suffix)) {
            isBaseFeature = false;
            break;
        }
    }
    if (isBaseFeature) {
        for (var j in cfg) {
            var defName = baseFeature.getName() + " " + cfg[j].suffix;
            var match = API.get("ProtocolUtils").getFeatureByName(defName, pClass);
            if (match == null) {
                createFeature(baseFeature, cfg[j]);
                featuresAdded = true;
            }
        }
    }
}

if (featuresAdded) API.get("ProtocolService").updateProtocolClass(pClass);
console.print("Done.");

function createFeature(baseFeature, featureDef) {
    var newFeatureName = baseFeature.getName() + " " + featureDef.suffix;
    console.print("Creating new feature " + newFeatureName);
    var newFeature = API.get("ProtocolService").createFeature(pClass);
    pClass.getFeatures().add(newFeature);
    newFeature.setName(newFeatureName);
    // Calculation
    if (featureDef.calcFormula != null) newFeature.setCalculationFormula(java.lang.String.format(featureDef.calcFormula, baseFeature.getName()));
    if (featureDef.calcFormulaId != null) newFeature.setCalculationFormulaId(featureDef.calcFormulaId);
    if (featureDef.calcSequence != null) newFeature.setCalculationSequence(featureDef.calcSequence);
    // Normalization
    if (featureDef.normName != null) newFeature.setNormalization(featureDef.normName);
    if (featureDef.normFormula != null) newFeature.setNormalizationFormula(featureDef.normFormula);
    // Dose-response curves
    if (featureDef.curveModel != null) newFeature.getCurveSettings().put("MODEL", featureDef.curveModel);
    if (featureDef.curveMethod != null) newFeature.getCurveSettings().put("Method", featureDef.curveMethod);
    if (featureDef.curveType != null) newFeature.getCurveSettings().put("Type", featureDef.curveType);
}