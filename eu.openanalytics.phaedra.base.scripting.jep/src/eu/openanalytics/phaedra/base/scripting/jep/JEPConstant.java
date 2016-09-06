package eu.openanalytics.phaedra.base.scripting.jep;

public enum JEPConstant {

	/** Value of mathematical Pi. */
	pi {
		public String getDescription() { return "Value of  \u03C0"; }
	},

	/** Value of mathematical E. */
	e {
		public String getDescription() { return "Value of e (base of natural logarithm)"; }
	};

	public abstract String getDescription();

}