package eu.openanalytics.phaedra.base.datatype.format;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import eu.openanalytics.phaedra.base.datatype.TestObj1;
import eu.openanalytics.phaedra.base.datatype.description.ConcentrationValueDescription;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


public class DataFormatterTest {
	
	
	@Test
	public void format_ConcentrationValue_LogMolar() {
		final DataFormatter dataFormatter = new DataFormatter(new ConcentrationFormat(ConcentrationUnit.LogMolar, 3),
				null, null );
		
		assertEquals("9", dataFormatter.format(1.0e-9, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.Molar)));
		assertEquals("8.824", dataFormatter.format(1.5e-9, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.Molar)));
		assertEquals("9", dataFormatter.format(9.0, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.LogMolar)));
		assertEquals("8.5", dataFormatter.format(8.5, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.LogMolar)));
	}
	
	@Test
	public void format_ConcentrationValue_Molar() {
		final DataFormatter dataFormatter = new DataFormatter(new ConcentrationFormat(ConcentrationUnit.Molar, 3),
				null, null );
		
		assertEquals("1E-9", dataFormatter.format(1.0e-9, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.Molar)));
		assertEquals("1.5E-9", dataFormatter.format(1.5e-9, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.Molar)));
		assertEquals("1E-9", dataFormatter.format(9.0, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.LogMolar)));
		assertEquals("3.162E-9", dataFormatter.format(8.5, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.LogMolar)));
	}
	
	@Test
	public void format_ConcentrationValue_typeSpecific() {
		Map<String, ConcentrationFormat> typeFormats = new HashMap<>();
		typeFormats.put(TestObj1.class.getName(), new ConcentrationFormat(ConcentrationUnit.LogMolar, 3));
		
		final DataFormatter dataFormatter = new DataFormatter(new ConcentrationFormat(ConcentrationUnit.Molar, 3),
				typeFormats, null );
		
		assertEquals("9", dataFormatter.format(1.0e-9, new ConcentrationValueDescription("conc", TestObj1.class, ConcentrationUnit.Molar)));
		assertEquals("8.824", dataFormatter.format(1.5e-9, new ConcentrationValueDescription("conc", TestObj1.class, ConcentrationUnit.Molar)));
		assertEquals("9", dataFormatter.format(9.0, new ConcentrationValueDescription("conc", TestObj1.class, ConcentrationUnit.LogMolar)));
		assertEquals("8.5", dataFormatter.format(8.5, new ConcentrationValueDescription("conc", TestObj1.class, ConcentrationUnit.LogMolar)));
		
		assertEquals("1E-9", dataFormatter.format(1.0e-9, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.Molar)));
		assertEquals("1.5E-9", dataFormatter.format(1.5e-9, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.Molar)));
		assertEquals("1E-9", dataFormatter.format(9.0, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.LogMolar)));
		assertEquals("3.162E-9", dataFormatter.format(8.5, new ConcentrationValueDescription("conc", Object.class, ConcentrationUnit.LogMolar)));
	}
	
}
