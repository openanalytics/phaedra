package eu.openanalytics.phaedra.datacapture.montage.layout;

import java.util.Arrays;

import eu.openanalytics.phaedra.base.imaging.util.Montage;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.montage.MontageConfig;

public class LiteralFieldLayoutSource extends BaseFieldLayoutSource {

	public FieldLayout test(String layout) {
		MontageConfig cfg = new MontageConfig();
		cfg.layout = layout;
		return getLayout(null, 0, cfg, null);
	}
	
	@Override
	public FieldLayout getLayout(PlateReading reading, int fieldCount, MontageConfig montageConfig, DataCaptureContext context) {

		int[][] parsedLayout = Montage.parseLayout(montageConfig.layout);
		int startingFieldNr = Arrays.stream(parsedLayout).flatMapToInt(i -> Arrays.stream(i)).filter(i -> i >= 0).min().orElse(0);
		FieldLayout fieldLayout = new FieldLayout(startingFieldNr);
		
		for (int r = 0; r < parsedLayout.length; r++) {
			for (int c = 0; c < parsedLayout[r].length; c++) {
				fieldLayout.addFieldPosition(parsedLayout[r][c], c, r);
			}
		}
		
		return fieldLayout;
	}
}
