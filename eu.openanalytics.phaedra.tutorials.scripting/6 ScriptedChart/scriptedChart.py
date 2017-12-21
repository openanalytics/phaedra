import io
import matplotlib as mpl
import matplotlib.pyplot as plt
mpl.use('Agg')

plateId = 119
wellPos = [2, 2]
f1Name = "AinNint"
f2Name = "AI1"

plate = API.get("PlateService").getPlateById(plateId)
well = API.get("PlateUtils").getWell(plate, wellPos[0], wellPos[1])
pclass = API.get("ProtocolUtils").getProtocolClass(plate)

f1 = API.get("ProtocolUtils").getSubWellFeatureByName(f1Name, pclass)
f2 = API.get("ProtocolUtils").getSubWellFeatureByName(f2Name, pclass)
d1 = API.get("SubWellService").getNumericData(well, f1)
d2 = API.get("SubWellService").getNumericData(well, f2)

fig = plt.figure(figsize=(width/100, height/100), dpi=100)
fig.add_subplot(1, 1, 1).scatter(d1, d2)

plt.title("Example scatterplot")
plt.xlabel(f1Name)
plt.ylabel(f2Name)

buffer = io.BytesIO()
fig.savefig(buffer, format='png')

for b in buffer.getvalue():
	output.write(ord(b));