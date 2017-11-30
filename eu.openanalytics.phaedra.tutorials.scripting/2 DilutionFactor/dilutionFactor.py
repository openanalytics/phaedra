plateId = int(str(args.get(0)))
dilutionFactor = float(str(args.get(1)))

plate = API.get("PlateService").getPlateById(plateId)
wells = plate.getWells().toArray()

for well in wells:
	newConc = well.getCompoundConcentration() / dilutionFactor
	well.setCompoundConcentration(newConc)

API.get("PlateService").updatePlate(plate)
API.get("CalculationService").calculate(plate)