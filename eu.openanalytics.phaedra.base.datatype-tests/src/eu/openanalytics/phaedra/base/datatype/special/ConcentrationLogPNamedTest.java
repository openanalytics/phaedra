package eu.openanalytics.phaedra.base.datatype.special;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.LogMolar;
import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.Molar;
import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.NanoMolar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.databinding.conversion.IConverter;
import org.junit.Test;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.TestObj1;
import eu.openanalytics.phaedra.base.datatype.description.BaseDataUnitConfig;
import eu.openanalytics.phaedra.base.datatype.description.ContentType;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;


public class ConcentrationLogPNamedTest {
	
	
	@Test
	public void properties_Value() {
		final ConcentrationLogPNamedValueDescription descr = new ConcentrationLogPNamedValueDescription("IC50", TestObj1.class, Molar);
		assertEquals("IC50", descr.getName());
		assertEquals(DataType.Real, descr.getDataType());
		assertEquals(ContentType.Concentration, descr.getContentType());
		assertEquals(TestObj1.class, descr.getEntityType());
		assertEquals(Molar, descr.getConcentrationUnit());
	}
	
	@Test
	public void properties_CensoredValue() {
		final ConcentrationLogPNamedCensoredValueDescription descr = new ConcentrationLogPNamedCensoredValueDescription("IC50", TestObj1.class, Molar, "IC50 Censor");
		assertEquals("IC50", descr.getName());
		assertEquals(DataType.Real, descr.getDataType());
		assertEquals(ContentType.Concentration, descr.getContentType());
		assertEquals(TestObj1.class, descr.getEntityType());
		assertEquals(Molar, descr.getConcentrationUnit());
		assertEquals("IC50 Censor", descr.getCensorName());
	}
	
	@Test
	public void properties_Censor() {
		final ConcentrationLogPNamedCensorDescription descr = new ConcentrationLogPNamedCensorDescription("IC50 Censor", TestObj1.class, Molar);
		assertEquals("IC50 Censor", descr.getName());
		assertEquals(DataType.String, descr.getDataType());
		assertEquals(ContentType.Concentration, descr.getContentType());
		assertEquals(TestObj1.class, descr.getEntityType());
		assertEquals(Molar, descr.getConcentrationUnit());
	}
	
	@Test
	public void properties_RelatedReal() {
		final RealValueConcentrationLogPNamedDescription descr = new RealValueConcentrationLogPNamedDescription("IC50 Eff", TestObj1.class, Molar);
		assertEquals("IC50 Eff", descr.getName());
		assertEquals(DataType.Real, descr.getDataType());
		assertEquals(ContentType.Other, descr.getContentType());
		assertEquals(TestObj1.class, descr.getEntityType());
		assertEquals(Molar, descr.getConcentrationUnit());
	}
	
	
	@Test
	public void alter_CensoredValue_MolarToMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(Molar, null);
		
		final ConcentrationLogPNamedCensoredValueDescription orgDescr = new ConcentrationLogPNamedCensoredValueDescription("IC50", TestObj1.class, Molar, "IC50 Censor");
		final ConcentrationLogPNamedCensoredValueDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertTrue(orgDescr == alteredDescr);
		assertNull(orgDescr.getDataConverterTo(targetConfig));
	}
	
	@Test
	public void alter_Censor_MolarToMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(Molar, null);
		
		final ConcentrationLogPNamedCensorDescription orgDescr = new ConcentrationLogPNamedCensorDescription("IC50 Censor", TestObj1.class, Molar);
		final ConcentrationLogPNamedCensorDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertTrue(orgDescr == alteredDescr);
		assertNull(orgDescr.getDataConverterTo(targetConfig));
	}
	
	@Test
	public void alter_RelatedReal_MolarToMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(Molar, null);
		
		final RealValueConcentrationLogPNamedDescription orgDescr = new RealValueConcentrationLogPNamedDescription("IC50 Eff", TestObj1.class, Molar);
		final RealValueConcentrationLogPNamedDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertTrue(orgDescr == alteredDescr);
		assertNull(orgDescr.getDataConverterTo(targetConfig));
	}
	
	@Test
	public void alter_CensoredValue_MolarToNanoMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(NanoMolar, null);
		
		final ConcentrationLogPNamedCensoredValueDescription orgDescr = new ConcentrationLogPNamedCensoredValueDescription("IC50", TestObj1.class, Molar, "IC50 Censor");
		final ConcentrationLogPNamedCensoredValueDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertEquals(ConcentrationLogPNamedCensoredValueDescription.class, alteredDescr.getClass());
		assertEquals("IC50", alteredDescr.getName());
		assertEquals(DataType.Real, alteredDescr.getDataType());
		assertEquals(ContentType.Concentration, alteredDescr.getContentType());
		assertEquals(TestObj1.class, alteredDescr.getEntityType());
		assertEquals(NanoMolar, alteredDescr.getConcentrationUnit());
		assertEquals("IC50 Censor", alteredDescr.getCensorName());
		final IConverter valueConverter = orgDescr.getDataConverterTo(targetConfig);
		assertNotNull(valueConverter);
		assertEquals(Double.class, valueConverter.getFromType());
		assertEquals(Double.class, valueConverter.getToType());
		assertEquals(1e-0, valueConverter.convert(1e-9));
	}
	
	@Test
	public void alter_Censor_MolarToNanoMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(NanoMolar, null);
		
		final ConcentrationLogPNamedCensorDescription orgDescr = new ConcentrationLogPNamedCensorDescription("IC50 Censor", TestObj1.class, Molar);
		final ConcentrationLogPNamedCensorDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertEquals(ConcentrationLogPNamedCensorDescription.class, alteredDescr.getClass());
		assertEquals("IC50 Censor", alteredDescr.getName());
		assertEquals(DataType.String, alteredDescr.getDataType());
		assertEquals(ContentType.Concentration, alteredDescr.getContentType());
		assertEquals(TestObj1.class, alteredDescr.getEntityType());
		assertEquals(NanoMolar, alteredDescr.getConcentrationUnit());
		final IConverter censorConverter = orgDescr.getDataConverterTo(targetConfig);
		assertNotNull(censorConverter);
		assertEquals(String.class, censorConverter.getFromType());
		assertEquals(String.class, censorConverter.getToType());
		assertEquals("<", censorConverter.convert("<"));
	}
	
	@Test
	public void alter_RelatedReal_MolarToNanoMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(NanoMolar, null);
		
		final RealValueConcentrationLogPNamedDescription orgDescr = new RealValueConcentrationLogPNamedDescription("IC50 Eff", TestObj1.class, Molar);
		final RealValueConcentrationLogPNamedDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertEquals(RealValueConcentrationLogPNamedDescription.class, alteredDescr.getClass());
		assertEquals("IC50 Eff", alteredDescr.getName());
		assertEquals(DataType.Real, alteredDescr.getDataType());
		assertEquals(ContentType.Other, alteredDescr.getContentType());
		assertEquals(TestObj1.class, alteredDescr.getEntityType());
		assertEquals(NanoMolar, alteredDescr.getConcentrationUnit());
		final IConverter censorConverter = orgDescr.getDataConverterTo(targetConfig);
		assertNull(censorConverter);
	}
	
	@Test
	public void alter_CensoredValue_MolarToLogMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(LogMolar, null);
		
		final ConcentrationLogPNamedCensoredValueDescription orgDescr = new ConcentrationLogPNamedCensoredValueDescription("IC50", TestObj1.class, Molar, "IC50 Censor");
		final ConcentrationLogPNamedCensoredValueDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertEquals(ConcentrationLogPNamedCensoredValueDescription.class, alteredDescr.getClass());
		assertEquals("pIC50", alteredDescr.getName());
		assertEquals(DataType.Real, alteredDescr.getDataType());
		assertEquals(ContentType.Concentration, alteredDescr.getContentType());
		assertEquals(TestObj1.class, alteredDescr.getEntityType());
		assertEquals(LogMolar, alteredDescr.getConcentrationUnit());
		assertEquals("pIC50 Censor", alteredDescr.getCensorName());
		final IConverter valueConverter = orgDescr.getDataConverterTo(targetConfig);
		assertNotNull(valueConverter);
		assertEquals(Double.class, valueConverter.getFromType());
		assertEquals(Double.class, valueConverter.getToType());
		assertEquals(9.0, valueConverter.convert(1e-9));
	}
	
	@Test
	public void alter_Censor_MolarToLogMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(LogMolar, null);
		
		final ConcentrationLogPNamedCensorDescription orgDescr = new ConcentrationLogPNamedCensorDescription("IC50 Censor", TestObj1.class, Molar);
		final ConcentrationLogPNamedCensorDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertEquals(ConcentrationLogPNamedCensorDescription.class, alteredDescr.getClass());
		assertEquals("pIC50 Censor", alteredDescr.getName());
		assertEquals(DataType.String, alteredDescr.getDataType());
		assertEquals(ContentType.Concentration, alteredDescr.getContentType());
		assertEquals(TestObj1.class, alteredDescr.getEntityType());
		assertEquals(LogMolar, alteredDescr.getConcentrationUnit());
		final IConverter censorConverter = orgDescr.getDataConverterTo(targetConfig);
		assertNotNull(censorConverter);
		assertEquals(String.class, censorConverter.getFromType());
		assertEquals(String.class, censorConverter.getToType());
		assertEquals(">", censorConverter.convert("<"));
	}
	
	@Test
	public void alter_RelatedReal_MolarToLogMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(LogMolar, null);
		
		final RealValueConcentrationLogPNamedDescription orgDescr = new RealValueConcentrationLogPNamedDescription("IC50 Eff", TestObj1.class, Molar);
		final RealValueConcentrationLogPNamedDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertEquals(RealValueConcentrationLogPNamedDescription.class, alteredDescr.getClass());
		assertEquals("pIC50 Eff", alteredDescr.getName());
		assertEquals(DataType.Real, alteredDescr.getDataType());
		assertEquals(ContentType.Other, alteredDescr.getContentType());
		assertEquals(TestObj1.class, alteredDescr.getEntityType());
		assertEquals(LogMolar, alteredDescr.getConcentrationUnit());
		final IConverter censorConverter = orgDescr.getDataConverterTo(targetConfig);
		assertNull(censorConverter);
	}
	
	@Test
	public void alter_CensoredValue_LogMolarToNanoMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(NanoMolar, null);
		
		final ConcentrationLogPNamedCensoredValueDescription orgDescr = new ConcentrationLogPNamedCensoredValueDescription("pIC50", TestObj1.class, LogMolar, "pIC50 Censor");
		final ConcentrationLogPNamedCensoredValueDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertEquals(ConcentrationLogPNamedCensoredValueDescription.class, alteredDescr.getClass());
		assertEquals("IC50", alteredDescr.getName());
		assertEquals(DataType.Real, alteredDescr.getDataType());
		assertEquals(ContentType.Concentration, alteredDescr.getContentType());
		assertEquals(TestObj1.class, alteredDescr.getEntityType());
		assertEquals(NanoMolar, alteredDescr.getConcentrationUnit());
		assertEquals("IC50 Censor", alteredDescr.getCensorName());
		final IConverter valueConverter = orgDescr.getDataConverterTo(targetConfig);
		assertNotNull(valueConverter);
		assertEquals(Double.class, valueConverter.getFromType());
		assertEquals(Double.class, valueConverter.getToType());
		assertEquals(1.0e-0, valueConverter.convert(9.0));
	}
	
	@Test
	public void alter_Censor_LogMolarToNanoMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(NanoMolar, null);
		
		final ConcentrationLogPNamedCensorDescription orgDescr = new ConcentrationLogPNamedCensorDescription("pIC50 Censor", TestObj1.class, LogMolar);
		final ConcentrationLogPNamedCensorDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertEquals(ConcentrationLogPNamedCensorDescription.class, alteredDescr.getClass());
		assertEquals("IC50 Censor", alteredDescr.getName());
		assertEquals(DataType.String, alteredDescr.getDataType());
		assertEquals(ContentType.Concentration, alteredDescr.getContentType());
		assertEquals(TestObj1.class, alteredDescr.getEntityType());
		assertEquals(NanoMolar, alteredDescr.getConcentrationUnit());
		final IConverter censorConverter = orgDescr.getDataConverterTo(targetConfig);
		assertNotNull(censorConverter);
		assertEquals(String.class, censorConverter.getFromType());
		assertEquals(String.class, censorConverter.getToType());
		assertEquals("<", censorConverter.convert(">"));
	}
	
	@Test
	public void alter_RelatedReal_LogMolarToNanoMolar() {
		final DataUnitConfig targetConfig = new BaseDataUnitConfig(NanoMolar, null);
		
		final RealValueConcentrationLogPNamedDescription orgDescr = new RealValueConcentrationLogPNamedDescription("pIC50 Eff", TestObj1.class, LogMolar);
		final RealValueConcentrationLogPNamedDescription alteredDescr = orgDescr.alterTo(targetConfig);
		assertEquals(RealValueConcentrationLogPNamedDescription.class, alteredDescr.getClass());
		assertEquals("IC50 Eff", alteredDescr.getName());
		assertEquals(DataType.Real, alteredDescr.getDataType());
		assertEquals(ContentType.Other, alteredDescr.getContentType());
		assertEquals(TestObj1.class, alteredDescr.getEntityType());
		assertEquals(NanoMolar, alteredDescr.getConcentrationUnit());
		final IConverter censorConverter = orgDescr.getDataConverterTo(targetConfig);
		assertNull(censorConverter);
	}
	
}
