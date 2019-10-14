package eu.openanalytics.phaedra.base.datatype.unit;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.LogMolar;
import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.MicroMolar;
import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.Molar;
import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.NanoMolar;
import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class ConcentrationUnitTest {
	
	
	@Test
	public void getAbbr() {
		assertEquals("M", Molar.getAbbr());
		assertEquals("μM", MicroMolar.getAbbr());
		assertEquals("nM", NanoMolar.getAbbr());
		assertEquals("-log(M)", LogMolar.getAbbr());
	}
	
	
	@Test
	public void convert_toMolar() {
		assertEquals(1.0e-9, Molar.convert(1.0e-9, Molar), 0);
		assertEquals(1.0e-9, Molar.convert(1.0e-3, MicroMolar), 0);
		assertEquals(1.0e-9, Molar.convert(1.0e-0, NanoMolar), 0);
		assertEquals(1.0e-9, Molar.convert(9, LogMolar), 0);
	}
	
	@Test
	public void convert_toMicroMolar() {
		assertEquals(1.0e-3, MicroMolar.convert(1.0e-9, Molar), 0);
		assertEquals(1.0e-3, MicroMolar.convert(1.0e-3, MicroMolar), 0);
		assertEquals(1.0e-3, MicroMolar.convert(1.0e-0, NanoMolar), 0);
		assertEquals(1.0e-3, MicroMolar.convert(9, LogMolar), 0);
	}
	
	@Test
	public void convert_toNanoMolar() {
		assertEquals(1.0e-0, NanoMolar.convert(1.0e-9, Molar), 0);
		assertEquals(1.0e-0, NanoMolar.convert(1.0e-3, MicroMolar), 0);
		assertEquals(1.0e-0, NanoMolar.convert(1.0e-0, NanoMolar), 0);
		assertEquals(1.0e-0, NanoMolar.convert(9, LogMolar), 0);
	}
	
	@Test
	public void convert_toLogMolar() {
		assertEquals(9, LogMolar.convert(1.0e-9, Molar), 0);
		assertEquals(9, LogMolar.convert(1.0e-3, MicroMolar), 0);
		assertEquals(9, LogMolar.convert(1.0e-0, NanoMolar), 0);
		assertEquals(9, LogMolar.convert(9, LogMolar), 0);
	}
	
	
	@Test
	public void convertCensor_toMolar() {
		assertEquals("<", Molar.convertCensor("<", Molar));
		assertEquals(">", Molar.convertCensor(">", Molar));
		assertEquals("~", Molar.convertCensor("~", Molar));
		assertEquals("<", Molar.convertCensor("<", MicroMolar));
		assertEquals(">", Molar.convertCensor(">", MicroMolar));
		assertEquals("~", Molar.convertCensor("~", MicroMolar));
		assertEquals("<", Molar.convertCensor("<", NanoMolar));
		assertEquals(">", Molar.convertCensor(">", NanoMolar));
		assertEquals("~", Molar.convertCensor("~", NanoMolar));
		assertEquals(">", Molar.convertCensor("<", LogMolar));
		assertEquals("<", Molar.convertCensor(">", LogMolar));
		assertEquals("~", Molar.convertCensor("~", LogMolar));
	}
	
	@Test
	public void convertCensor_toMicroMolar() {
		assertEquals("<", MicroMolar.convertCensor("<", Molar));
		assertEquals(">", MicroMolar.convertCensor(">", Molar));
		assertEquals("~", MicroMolar.convertCensor("~", Molar));
		assertEquals("<", MicroMolar.convertCensor("<", MicroMolar));
		assertEquals(">", MicroMolar.convertCensor(">", MicroMolar));
		assertEquals("~", MicroMolar.convertCensor("~", MicroMolar));
		assertEquals("<", MicroMolar.convertCensor("<", NanoMolar));
		assertEquals(">", MicroMolar.convertCensor(">", NanoMolar));
		assertEquals("~", MicroMolar.convertCensor("~", NanoMolar));
		assertEquals(">", MicroMolar.convertCensor("<", LogMolar));
		assertEquals("<", MicroMolar.convertCensor(">", LogMolar));
		assertEquals("~", MicroMolar.convertCensor("~", LogMolar));
	}
	
	@Test
	public void convertCensor_toNanoMolar() {
		assertEquals("<", NanoMolar.convertCensor("<", Molar));
		assertEquals(">", NanoMolar.convertCensor(">", Molar));
		assertEquals("~", NanoMolar.convertCensor("~", Molar));
		assertEquals("<", NanoMolar.convertCensor("<", MicroMolar));
		assertEquals(">", NanoMolar.convertCensor(">", MicroMolar));
		assertEquals("~", NanoMolar.convertCensor("~", MicroMolar));
		assertEquals("<", NanoMolar.convertCensor("<", NanoMolar));
		assertEquals(">", NanoMolar.convertCensor(">", NanoMolar));
		assertEquals("~", NanoMolar.convertCensor("~", NanoMolar));
		assertEquals(">", NanoMolar.convertCensor("<", LogMolar));
		assertEquals("<", NanoMolar.convertCensor(">", LogMolar));
		assertEquals("~", NanoMolar.convertCensor("~", LogMolar));
	}
	
	@Test
	public void convertCensor_toLogMolar() {
		assertEquals(">", LogMolar.convertCensor("<", Molar));
		assertEquals("<", LogMolar.convertCensor(">", Molar));
		assertEquals("~", LogMolar.convertCensor("~", Molar));
		assertEquals(">", LogMolar.convertCensor("<", MicroMolar));
		assertEquals("<", LogMolar.convertCensor(">", MicroMolar));
		assertEquals("~", LogMolar.convertCensor("~", MicroMolar));
		assertEquals(">", LogMolar.convertCensor("<", NanoMolar));
		assertEquals("<", LogMolar.convertCensor(">", NanoMolar));
		assertEquals("~", LogMolar.convertCensor("~", NanoMolar));
		assertEquals("<", LogMolar.convertCensor("<", LogMolar));
		assertEquals(">", LogMolar.convertCensor(">", LogMolar));
		assertEquals("~", LogMolar.convertCensor("~", LogMolar));
	}
	
}
