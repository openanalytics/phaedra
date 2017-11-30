var plateId = parseInt(args[0]);
var dilutionFactor = parseFloat(args[1]);

var plate = API.get("PlateService").getPlateById(plateId);
var wells = plate.getWells().toArray();
for (var i in wells) {
	var newConc = wells[i].getCompoundConcentration() / dilutionFactor;
	wells[i].setCompoundConcentration(newConc);
}

API.get("PlateService").updatePlate(plate);
API.get("CalculationService").calculate(plate);