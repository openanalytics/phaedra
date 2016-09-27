package eu.openanalytics.phaedra.model.curve.osb;

import eu.openanalytics.phaedra.model.curve.CurveFitErrorCode;

public class OSBErrorCodes {
	
	public static final CurveFitErrorCode[] CODES = {
			
		new CurveFitErrorCode(-3, ""
			+ "- No data have been provided\n"
			+ "- One of the following arguments have not been provided to the drcFit function: model, conc, type, accept\n"
			+ "- The number of concentration values, response values and accept values are not equal\n"
			+ "- The model specifcation string is not one of 'PL2', 'PL3L', 'PL3U', 'PL4', 'PL2H1', 'PL3LH1', 'PL3UH1', 'PL4H1', 'TSB2', 'TSB3L', 'TSB3U' or 'TSB4'\n"
			+ "- The type argument is not one of 'A' (ascending) or 'D' (descending)\n"
			+ "- Lower bound not given for a model which requires it\n"
			+ "- Upper bound not given for a model which requires it\n"
			+ "- The upper bound that has been provided by the user (or computed by using the maximum observation) is less than\n"
			+ "or equal to the lower bound provided by the user (or computed by using the minimum observation)\n"
			+ "- There are no data points between the fixed asymptotes provided by the user"),
		
		new CurveFitErrorCode(-2, "" 
			+ "A first situation is the situation where no convergence is achieved for non-linear least squares fitting of non-PL2 models.\n\n"
			+ "A second situation is the situation where no convergence is achieved for non-linear least squares fitting of PL2 models.\n"
			+ "In that case an attempt will be made to estimate the parameters using linear interpolation.\n"
			+ "If the condition for linear interpolation is not met an error code of -2 is generated.\n\n"
			+ "A third situation is the situation where there are less concentrations than np+1 where np is the number of parameters of the model chosen.\n"
			+ "In that case a linear interpolation is attempted. If the condition for linear interpolation (cf. item above) is not met, an error code of -2 is generated."),
		
		new CurveFitErrorCode(-1, ""
			+ "Irregularities are detected within the drcCheckDoses function; The named irregularities (minimum above 50%, maximum below 50%,\n"
			+ "all data within the 46-54% range, one or more responses at the extreme of the concentration range are equal to 50% when rounded to two digits)\n"
			+ "are described in the drcCheckDoses section of the chapter on censor values.\n"
			+ "The value will be -1 if it concerns a model for which both limits are fixed and which is part of the TSB family of models.\n"
			+ "The value will be -1 as well if it concerns a model for which the limits are not fixed.\n\n"
			+ "If the number of unique concentrations is less than the number of parameters plus one and the model is not a PL2 model the ERROR code is set to -1.\n"
			+ "This occurs in the drcOLSFit2 function."),
		
		new CurveFitErrorCode(1, ""
			+ "Irregularities are detected within the drcCheckDoses function; The named irregularities (minimum above 50%, maximum below 50%,\n"
			+ "all data within the 46-54% range, one or more responses at the extreme of the concentration range are equal to 50% when rounded to two digits)\n"
			+ "are described in the drcCheckDoses section of the chapter on censor values.\n"
			+ "The value will be 1 if it concerns a model for which both limits are fixed and which is not part of the TSB family of models.\n\n"
			+ "If the number of unique concentrations is smaller than the number of parameters plus one and the model is a PL2 model,\n"
			+ "a linear interpolation is applied. If the requirements for performing a linear interpolation are met, the value is set to 1.\n"
			+ "This situation is dealt with in the drcOLSFit2 function.\n\n"
			+ "A PL2 model is fit to the data using NLS, but does not reach convergence; in this scenario a linear interpolation is applied.\n"
			+ "If the requirements for performing a linear interpolation are met, the value is set to 1. This situation is dealt with in the drcOLSFit2 function.\n\n"
			+ "The NLS fit to the data converges, but the resulting PIC50 value is either 0.05 smaller than the smallest observed concentration\n"
			+ "or 0.05 greater than the highest observed concentration. This situation is dealt with in the drcOLSFit2 function.")
	};
}
