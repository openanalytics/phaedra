package eu.openanalytics.phaedra.model.plate.util;

import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class PlateSummary {

	public int crcCount;
	public int screenCount;

	public double getStat(String stat, Feature f, String wellType, String norm) {
		return Double.NaN;
	}
}
