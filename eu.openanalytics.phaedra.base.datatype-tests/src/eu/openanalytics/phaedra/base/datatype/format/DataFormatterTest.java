package eu.openanalytics.phaedra.base.datatype.format;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import eu.openanalytics.phaedra.base.datatype.description.ConcentrationValueDescription;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


public class DataFormatterTest {
	
	
	@Test
	public void format_ConcentrationValue_LogMolar() {
		final DataFormatter dataFormatter = new DataFormatter(new ConcentrationFormat(ConcentrationUnit.LogMolar, 3));
		
		assertEquals("9", dataFormatter.format(1.0e-9, new ConcentrationValueDescription("conc", ConcentrationUnit.Molar)));
		assertEquals("8.824", dataFormatter.format(1.5e-9, new ConcentrationValueDescription("conc", ConcentrationUnit.Molar)));
		assertEquals("9", dataFormatter.format(9.0, new ConcentrationValueDescription("conc", ConcentrationUnit.LogMolar)));
		assertEquals("8.5", dataFormatter.format(8.5, new ConcentrationValueDescription("conc", ConcentrationUnit.LogMolar)));
	}
	
	@Test
	public void format_ConcentrationValue_Molar() {
		final DataFormatter dataFormatter = new DataFormatter(new ConcentrationFormat(ConcentrationUnit.Molar, 3));
		
		assertEquals("1E-9", dataFormatter.format(1.0e-9, new ConcentrationValueDescription("conc", ConcentrationUnit.Molar)));
		assertEquals("1.5E-9", dataFormatter.format(1.5e-9, new ConcentrationValueDescription("conc", ConcentrationUnit.Molar)));
		assertEquals("1E-9", dataFormatter.format(9.0, new ConcentrationValueDescription("conc", ConcentrationUnit.LogMolar)));
		assertEquals("3.162E-9", dataFormatter.format(8.5, new ConcentrationValueDescription("conc", ConcentrationUnit.LogMolar)));
	}
	
}
