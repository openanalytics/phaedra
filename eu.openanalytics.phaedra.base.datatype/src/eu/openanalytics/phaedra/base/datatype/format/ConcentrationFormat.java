package eu.openanalytics.phaedra.base.datatype.format;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.LogMolar;

import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


/**
 * Format(ter) for concentration values.
 * 
 * The formatter is immutable and thread-safe.
 */
public class ConcentrationFormat {
	
	
	private final ConcentrationUnit unit;
	private final int decimals;
	
	private volatile String formatString;
	
	
	public ConcentrationFormat(final ConcentrationUnit unit, final int decimals) {
		this.unit = unit;
		this.decimals = decimals;
	}
	
	
	public ConcentrationUnit getUnit() {
		return this.unit;
	}
	
	public int getDecimals() {
		return this.decimals;
	}
	
	
	/**
	 * @param censor the censor, or <code>null</code> if the value is not censored.
	 * @param conc concentration value
	 * @param unit source unit of censor and conc
	 * @return the formatted concentration, possibly censored.
	 */
	public String format(/*@Nullable*/ final String censor, final double conc, final ConcentrationUnit unit) {
		final String formattedConc = format(this.unit.convert(conc, unit));
		return (censor != null) ?
				this.unit.convertCensor(censor, unit) + formattedConc :
				formattedConc;
	}

	/**
	 * @param conc concentration value
	 * @param unit source unit of conc
	 * @return the formatted concentration.
	 */
	public String format(final double conc, final ConcentrationUnit unit) {
		return format(this.unit.convert(conc, unit));
	}
	
	private String format(final double conc) {
		if (Double.isNaN(conc)) {
			return String.valueOf(conc);
		}
		
		String formatString = this.formatString;
		if (formatString == null) {
			formatString = createFormatString(this.decimals, this.unit != LogMolar);
			this.formatString = formatString;
		}
		return Formatter.getInstance().format(conc, formatString);
	}
	
	private String createFormatString(final int decimals, final boolean exp) {
		if (exp) {
			final StringBuilder sb = new StringBuilder("0.");
			for (int i = 0; i < decimals; i++) {
				sb.append("#");
			}
			sb.append("E0");
			return sb.toString();
		} else {
			if (decimals == 0) {
				return "#";
			}
			final StringBuilder sb = new StringBuilder("#.");
			for (int i = 0; i < decimals; i++) {
				sb.append("#");
			}
			return sb.toString();
		}
	}
	
	
	@Override
	public int hashCode() {
		int hash = this.unit.hashCode();
		hash = 31 * hash + this.decimals;
		return hash;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && getClass() == obj.getClass()) {
			final ConcentrationFormat other = (ConcentrationFormat)obj;
			return (this.unit == other.unit
					&& this.decimals == other.decimals);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return this.unit + "," + this.decimals;
	}
	
}
