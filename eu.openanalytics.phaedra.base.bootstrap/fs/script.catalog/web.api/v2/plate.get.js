load("script://web.api/header.js");

var plateId = parseInt(urlParts[0]);
var plate = API.get("PlateService").getPlateById(plateId);

var formatDate = function(date) {
  if (date == null) return null;
  return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date);
}

var plateObj = {
    id: plate.getId(),
    barcode: plate.getBarcode(),
    rows: plate.getRows(),
    columns: plate.getColumns(),
    sequence: plate.getSequence(),
    info: plate.getInfo(),
    description: plate.getDescription(),
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
    properties: {},
    wells: []
};

var plateProps = API.get("PlateService").getPlateProperties(plate);
for (var i in plateProps) {
  plateObj.properties[i] = plateProps[i];
}

var wells = plate.getWells().toArray();
for (var i in wells) {
    var well = wells[i];
    var wellNr = API.get("NumberUtils").getWellNr(well.getRow(), well.getColumn(), plate.getColumns());
    plateObj.wells[wellNr-1] = {
        id: well.getId(),
        row: well.getRow(),
        column: well.getColumn(),
        wellNr: wellNr,
        status: well.getStatus(),
        description: well.getDescription(),
        wellType: well.getWellType(),
        compoundId: (well.getCompound() == null) ? null : well.getCompound().getId(),
        compoundType: (well.getCompound() == null) ? null : well.getCompound().getType(),
        compoundNumber: (well.getCompound() == null) ? null : well.getCompound().getNumber(),
        compoundSaltform: (well.getCompound() == null) ? null : well.getCompound().getSaltform(),
        concentration: well.getCompoundConcentration()
    };
}

response.setContentType("application/json");
response.getWriter().write(JSON.stringify(plateObj, null, 2));