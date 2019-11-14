package eu.openanalytics.phaedra.base.datatype.unit;


public enum ConcentrationUnit {
	
	Molar("Molar", "M"),
	MicroMolar("Micromolar", "μM"),
	NanoMolar("Nanomolar", "nM"),
	LogMolar("Negative log of molar", "-log(M)");
	
	
	private final String abbr;
	private final String label;
	
	
	private ConcentrationUnit(final String label, final String abbr) {
		this.abbr = abbr;
		this.label = label;
	}
	
	
	public String getAbbr() {
		return this.abbr;
	}
	
	public String getLabel(final boolean withAbbr) {
		return (withAbbr) ?
				String.format("%1$s (%2$s)", this.label, this.abbr) :
				this.label;
	}
	
	
	/**
	 * Converts the specified concentration value.
	 * 
	 * @param conc the concentration value to convert.
	 * @param unit the unit of conc.
	 * @return the value converted to this unit.
	 */
	public double convert(final double conc, final ConcentrationUnit unit) {
		if (unit == this) {
			return conc;
		}
		final double molarConc = unit.toMolar(conc);
		return concFromMolar(molarConc);
	}
	
	/**
	 * Converts the specified censor of a concentration value.
	 * 
	 * @param censor the censor to convert.
	 * @param unit the unit of censor.
	 * @return the censor converted to this unit.
	 */
	public String convertCensor(final String censor, final ConcentrationUnit unit) {
		if (unit == this) {
			return censor;
		}
		final String molarCensor = unit.toMolarCensor(censor);
		return censorFromMolar(molarCensor);
	}
	
	
	public double toMolar(final double conc) {
		switch(this) {
		case Molar:
			return conc;
		case MicroMolar:
			return conc * 1e-6;
		case NanoMolar:
			return conc * 1e-9;
		case LogMolar:
			return Math.pow(10, -conc);
		default:
			throw new IllegalStateException();
		}
	}
	
	private double concFromMolar(final double molarConc) {
		switch(this) {
		case Molar:
			return molarConc;
		case MicroMolar:
			return molarConc * 1e+6;
		case NanoMolar:
			return molarConc * 1e+9;
		case LogMolar:
			return -Math.log10(molarConc);
		default:
			throw new IllegalStateException();
		}
	}
	
	
	public String toMolarCensor(final String censor) {
		switch(this) {
		case Molar:
		case MicroMolar:
		case NanoMolar:
			return censor;
		case LogMolar:
			return invertCensor(censor);
		default:
			throw new IllegalStateException();
		}
	}
	
	private String censorFromMolar(final String molarCensor) {
		switch(this) {
		case Molar:
		case MicroMolar:
		case NanoMolar:
			return molarCensor;
		case LogMolar:
			return invertCensor(molarCensor);
		default:
			throw new IllegalStateException();
		}
	}
	
	private String invertCensor(final String censor) {
		switch (censor) {
		case "<":
			return ">";
		case ">":
			return "<";
		default:
			return censor;
		}
	}
	
}
