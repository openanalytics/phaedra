package eu.openanalytics.phaedra.model.curve.util;

import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;


/**
 * @deprecated use {@link eu.openanalytics.phaedra.base.datatype.format.ConcentrationFormat}
 */
@Deprecated
public enum ConcentrationFormat {

	Molar(ConcentrationUnit.Molar),
	MicroMolar(ConcentrationUnit.MicroMolar),
	LogMolar(ConcentrationUnit.LogMolar);

	private ConcentrationUnit unit;

	private final static int DEFAULT_DECIMALS = 3;

	private ConcentrationFormat(ConcentrationUnit unit) {
		this.unit = unit;
	}
	
	
	public ConcentrationUnit getUnit() {
		return unit;
	}
	
	public String getLabel() {
		return unit.getAbbr();
	}

	/**
	 * Convert a (possibly censored) concentration from one format to another.
	 * 
	 * @param from The format that the concentration is currently given in.
	 * @param to The requested format.
	 * @param censor The censor, or null if the value is not censored.
	 * @param conc The concentration value.
	 * @return The formatted concentration, possibly censored.
	 */
	public static String format(ConcentrationFormat from, ConcentrationFormat to, String censor, double conc) {
		String molarCensor = from.censorToMolar(censor);
		double molarConc = from.concToMolar(conc);
		String formattedCensor = to.formatCensor(molarCensor);
		String formattedConc = to.format(molarConc, DEFAULT_DECIMALS);
		if (formattedCensor != null) formattedConc = formattedCensor + formattedConc;
		return formattedConc;
	}

	/**
	 * Decorate the name of a concentration value (e.g. IC50, LAC) to indicate its format.
	 * For example, a LogMolar format will add a lowercase 'p' in front of the name.
	 * 
	 * @param name The name to decorate.
	 * @return The decorated name.
	 */
	public String decorateName(String name) {
		// First, remove previous decorations.
		if (name.startsWith("p")) name = name.substring(1);
		if (name.endsWith(" (µM)")) name = name.substring(0, name.length()-5);
		if (name.endsWith(" (M)")) name = name.substring(0, name.length()-4);

		switch(this) {
		case Molar:
			return name + " (M)";
		case MicroMolar:
			return name + " (µM)";
		case LogMolar:
			//TODO Get rid of these hardcoded names.
			if (name.equalsIgnoreCase("ic50") || name.equalsIgnoreCase("lac")) return "p" + name;
		}
		return name;
	}
	
	/*
	 * Non-public
	 * **********
	 */

	private double concToMolar(double conc) {
		switch(this) {
		case Molar:
			return conc;
		case MicroMolar:
			return conc * 1E-6;
		case LogMolar:
			return Math.pow(10, -conc);
		}
		return conc;
	}

	private String censorToMolar(String censor) {
		switch(this) {
		case Molar:
			return censor;
		case MicroMolar:
			return censor;
		case LogMolar:
			return invertCensor(censor);
		}
		return censor;
	}
	
	private String format(double molarConc, int decimals) {
		if (Double.isNaN(molarConc)) return String.valueOf(molarConc);
		switch(this) {
		case Molar:
			return doFormat(molarConc, decimals, true);
		case MicroMolar:
			double mmConc = molarConc * 1E6;
			return doFormat(mmConc, decimals, true);
		case LogMolar:
			double logConc = -Math.log10(molarConc);
			return doFormat(logConc, decimals, false);
		}
		return doFormat(molarConc, decimals, true);
	}

	private String formatCensor(String molarCensor) {
		if (this == LogMolar) return invertCensor(molarCensor);
		return molarCensor;
	}
	
	private String invertCensor(String censor) {
		if ("<".equals(censor)) return ">";
		else if (">".equals(censor)) return "<";
		return censor;
	}

	private String doFormat(double conc, int decimals, boolean exp) {
		return Formatters.getInstance().format(conc, createFormatString(decimals, exp));
	}

	private String createFormatString(int decimals, boolean exp) {
		if (exp) {
			StringBuilder sb = new StringBuilder("0.");
			for (int i=0; i<decimals; i++) sb.append("#");
			sb.append("E0");
			return sb.toString();
		} else {
			if (decimals == 0) return "#";
			StringBuilder sb = new StringBuilder("#.");
			for (int i=0; i<decimals; i++) sb.append("#");
			return sb.toString();
		}
	}
}
