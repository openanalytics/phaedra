// API.get("ScriptService").getCatalog().run("misc/generateCalcFeature", [618, "CellCount", "ColEffect", "#%s#", 1, 0, null, "formula#1", null, null, null])

// Input parameters
var pClass = API.get("ProtocolService").getProtocolClass(pClassId);
if (pClass == null) throw "Protocol Class not found for ID: " + pClassId;
console.print("Checking features for protocol class " + pClass);

var baseFeature = API.get("ProtocolUtils").getFeatureByName(baseFeatureName, pClass);
if (baseFeature == null) throw "Feature not found for given name: " + baseFeatureName;

var featuresAdded = false;
var calcFeatureName = baseFeature.getName() + " " + featureConf.featureSuffix;
var calcFeature = API.get("ProtocolUtils").getFeatureByName(calcFeatureName, pClass);

if (calcFeature == null) {
   createFeature(baseFeature, featureConf);
   featuresAdded = true;
}

if (featuresAdded) API.get("ProtocolService").updateProtocolClass(pClass);
console.print("Done.");

function createFeature(baseFeature, featureDef) {
   var newFeatureName = baseFeature.getName() + " " + featureDef.suffix;
   console.print("Creating new feature " + newFeatureName);
   var newFeature = API.get("ProtocolService").createFeature(pClass);
   pClass.getFeatures().add(newFeature);
   newFeature.setName(newFeatureName);
   // General
   if (featureDef.keyFeature) newFeature.setKey(featureDef.keyFeature);
   // Calculation
//   if (featureDef.calcFormula != null) newFeature.setCalculationFormula(java.lang.String.format(featureDef.calcFormula, baseFeature.getName()));
   if (featureDef.calcFormula != null) newFeature.setCalculationFormula(featureDef.calcFormula.replace("${base.feature}", baseFeature.getName()));
   if (featureDef.calcFormulaId != null) newFeature.setCalculationFormulaId(featureDef.calcFormulaId);
   if (featureDef.calcSequence != null) newFeature.setCalculationSequence(featureDef.calcSequence);
   // Normalization
   if (featureDef.normalizationName != null) newFeature.setNormalization(featureDef.normalizationName);
   if (featureDef.normalizationFormula != null) newFeature.setNormalizationFormula(featureDef.normalizationFormula);
   // Dose-response curves
   if (featureDef.curveModel != null) newFeature.getCurveSettings().put("MODEL", featureDef.curveModel);
   if (featureDef.curveMethod != null) newFeature.getCurveSettings().put("Method", featureDef.curveMethod);
   if (featureDef.curveType != null) newFeature.getCurveSettings().put("Type", featureDef.curveType);
}