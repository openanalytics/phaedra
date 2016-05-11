package eu.openanalytics.phaedra.export.core.filter;

import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public class CompoundFilter {
	
	public CompoundFilter(List<Experiment> experiments) {
		// Do nothing.
	}
		
	public CompoundNr getNr(String type, String nr) {
		CompoundNr number = new CompoundNr();
		number.type = type;
		number.number = nr;
		return number;
	}
	
	public static class CompoundNr {
		public String type;
		public String number;
	}
}
