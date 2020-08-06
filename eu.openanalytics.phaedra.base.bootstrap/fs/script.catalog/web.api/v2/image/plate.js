var env = Java.type("eu.openanalytics.phaedra.base.environment.Screening").getEnvironment();
var plate = env.getEntityManager().find(Java.type("eu.openanalytics.phaedra.model.plate.vo.Plate").class, new java.lang.Long(id));

var ImageData = Java.type("org.eclipse.swt.graphics.ImageData");
var PaletteData = Java.type("org.eclipse.swt.graphics.PaletteData");
var palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
var IntArray = Java.type("int[]");

var size = 30;
if (queryArgs.size != null) size = parseInt(queryArgs.size);
var wellSize = [size, size];
image = new ImageData(plate.getColumns() * (wellSize[0] + 1), plate.getRows() * (wellSize[1] + 1), 24, palette);

for (var r = 1; r <= plate.getRows(); r++) {
    for (var c = 1; c <= plate.getColumns(); c++) {
        var well = API.get("PlateUtils").getWell(plate, r, c);
        var imgReq = generateImageRequest(well);
        imgReq.size = new org.eclipse.swt.graphics.Point(wellSize[0], wellSize[1]);
        wellImage = API.get("ImageRenderService").getImageData(imgReq);

        var offset = [(c-1) * (wellSize[0] + 1), (r-1) * (wellSize[1] + 1)];
        var lineBuffer = new IntArray(wellSize[0]);
        for (var yLine = 0; yLine < wellSize[1]; yLine++) {
			wellImage.getPixels(0, yLine, lineBuffer.length, lineBuffer, 0);
			image.setPixels(offset[0], offset[1] + yLine, lineBuffer.length, lineBuffer, 0);
		}
    }
}
