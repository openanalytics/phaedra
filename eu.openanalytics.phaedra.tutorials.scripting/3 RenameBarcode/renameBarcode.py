import os

for reading in ctx.getReadings():
	barcode = reading.getBarcode()
	parentFolderPath = os.path.dirname(reading.getSourcePath())
	parentFolderName = os.path.basename(parentFolderPath)
	barcode = barcode + "_" + parentFolderName[0:4]
	reading.setBarcode(barcode)