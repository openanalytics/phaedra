load("script://web.api/header.js");

var formatDate = function(date) {
    if (date == null) return null;
    return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date);
}

var barcodes = request.getParameter("barcode");
barcodes = barcodes.substring("in(".length, barcodes.length() - 1).split(",");

for (var i in barcodes) {
  barcodes[i] = "'" + barcodes[i] + "'";
}
var barcodeString = barcodes.join();

var queryString = "select barcode, min(import_date) as import_date from phaedra.hca_plate_import_timestamps where barcode"
    + " in (" + barcodeString + ") group by barcode";

var result = [];
var startTime = new Date();

// Look up the import timestamp of this plate.
var conn = jdbc.getPhaedraConnection();
try {
  jdbc.runQuery(conn, queryString, function(rs) {
    while (rs.next()) {
        result.push({
            barcode: rs.getString("barcode"),
            importDate: formatDate(rs.getTimestamp("import_date"))
        });
    }
  });
} catch (err) {
  // WIP, hca_plate_import_timestamps view may not exist in target schema yet.
  throw err;
} finally {
  conn.close();
}

var elapsed = new Date() - startTime;
http.logRequest("Executed query [" + elapsed + "ms] [" + result.length + " results]: " + queryString);

http.replyOk(response, result);