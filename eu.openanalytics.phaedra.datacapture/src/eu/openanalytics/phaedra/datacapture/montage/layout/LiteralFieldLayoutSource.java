package eu.openanalytics.phaedra.datacapture.montage.layout;

import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.montage.MontageConfig;

public class LiteralFieldLayoutSource extends BaseFieldLayoutSource {

	@Override
	public FieldLayout getLayout(PlateReading reading, int fieldCount, MontageConfig montageConfig, DataCaptureContext context) {

		String layout = montageConfig.layout;
		if (layout.startsWith("[")) layout = layout.substring(1);
		if (layout.endsWith("]")) layout = layout.substring(0, layout.length()-1);
		String[] rows = layout.split(";");
		int rowCount = rows.length;
		if (rows.length == 0) throw new IllegalArgumentException("Cannot montage, invalid layout: " + layout);
		int columnCount = rows[0].trim().split(",").length;

		int startingFieldNr = 10000;
		for (int r=0; r<rowCount; r++) {
			String[] columns = rows[r].trim().split(",");
			for (int c=0; c<columnCount; c++) {
				int fieldNr = Integer.parseInt(columns[c]);
				startingFieldNr = Math.min(startingFieldNr, fieldNr);
			}
		}
		
		FieldLayout fieldLayout = new FieldLayout(startingFieldNr);
		for (int r=0; r<rowCount; r++) {
			String[] columns = rows[r].trim().split(",");
			for (int c=0; c<columnCount; c++) {
				int fieldNr = Integer.parseInt(columns[c]);
				fieldLayout.addFieldPosition(fieldNr, c, r);
			}
		}
		return fieldLayout;
	}
}
