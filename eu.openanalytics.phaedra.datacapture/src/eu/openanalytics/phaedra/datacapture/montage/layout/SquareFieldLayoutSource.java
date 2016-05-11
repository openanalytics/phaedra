package eu.openanalytics.phaedra.datacapture.montage.layout;

import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.montage.MontageConfig;

public class SquareFieldLayoutSource extends BaseFieldLayoutSource {

	@Override
	public FieldLayout getLayout(PlateReading reading, int fieldCount, MontageConfig montageConfig, DataCaptureContext context) {
		
		// Determine smallest square-like layout that fits the number of fields
		int rows = 1;
		int columns = 1;
		while (rows*columns < fieldCount) {
			if (rows == columns) columns++;
			else rows++;
		}
		
		// Fill out the layout numbers
		FieldLayout layout = new FieldLayout(montageConfig.startingFieldNr);
		int fieldNr = montageConfig.startingFieldNr;
		for (int r=0; r<rows; r++) {
			for (int c=0; c<columns; c++) {
				int nr = (fieldNr < fieldCount+montageConfig.startingFieldNr) ? fieldNr++ : -1;
				layout.addFieldPosition(nr, c, r);
			}
		}
		return layout;
	}
}
