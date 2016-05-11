package eu.openanalytics.phaedra.calculation.jep;

public enum JEPFunction {
	/** Natural logarithm. */
	ln {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Natural Logarithm";
		}
	},
	/** Logarithm base 10. */
	log {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Logarithm base 10";
		}
	},
	/** GLOG. */
	glog {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Apply the GLOG"
					+ "\nThe percentile to use can be added as a second parameter (5 by default)";
		}
	},
	/** Exponential. */
	exp {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Exponential";
		}
	},
	/** Absolute Value / Magnitude. */
	abs {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Absolute Value / Magnitude";
		}
	},
	/** Square Root. */
	sqrt {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Square Root";
		}
	},
	/** Random number (between 0 and 1). */
	rand {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Random number (between 0 and 1)";
		}

		/** {@inheritDoc} */
		@Override
		public int getNrArgs() {
			return 0;
		}
	},
	/** Modulus. */
	mod {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Modulus: mod(dividend,divisor)"
					+ "\nFor each dividend value there has to be a divisor value";
		}

		/** {@inheritDoc} */
		@Override
		public int getNrArgs() {
			return 2;
		}
	},
	/** If-Condition. */
	iff {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "If-Condition: if (cond, trueval, falseval)";
		}

		/** {@inheritDoc} */
		@Override
		public int getNrArgs() {
			return 3;
		}

		/** {@inheritDoc} */
		@Override
		public String getFunctionName() {
			return "if";
		}
	},
	/** Round. */
	round {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Round: round(integer)";
		}
	},
	/** round(value, precision). */
	round_precision {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Round: round(value, precision)"
					+ "\nFor each value there has to be a precision value";
		}

		/** {@inheritDoc} */
		@Override
		public int getNrArgs() {
			return 2;
		}

		/** {@inheritDoc} */
		@Override
		public String getFunctionName() {
			return "round";
		}
	},
	/** Ceil. */
	ceil {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Smallest integer above the "
					+ "number; e.g. ceil(pi) returns 4";
		}
	},
	/** Floor. */
	floor {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Largest integer below the "
					+ "number; e.g. floor(pi) returns 3";
		}
	},
	/** Binomial coefficients. */
	binom {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Binomial coefficients";
		}

		/** {@inheritDoc} */
		@Override
		public int getNrArgs() {
			return 2;
		}
	},
	/** Sine. */
	sin {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Sine";
		}
	},
	/** Cosine. */
	cos {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Cosine";
		}
	},
	/** Tangent. */
	tan {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Tangent";
		}
	},
	/** Arc Sine. */
	asin {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Arc Sine";
		}
	},
	/** Arc Cosine. */
	acos {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Arc Cosine";
		}
	},
	/** Arc Tangent. */
	atan {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Arc Tangent";
		}
	},
	/** Arc Tangent 2 parameters. */
	atan2 {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Arc Tangent (with 2 parameters)";
		}

		@Override
		public int getNrArgs() {
			return 2;
		}
	},
	/** Hyperbolic Sine. */
	sinh {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Hyperbolic Sine";
		}
	},
	/** Hyperbolic Cosine. */
	cosh {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Hyperbolic Cosine";
		}
	},
	/** Hyperbolic Tangent. */
	tanh {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Hyperbolic Tangent";
		}
	},
	/** Inverse Hyperbolic Sine. */
	asinh {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Inverse Hyperbolic Sine";
		}
	},
	/** Inverse Hyperbolic Cosine. */
	acosh {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Inverse Hyperbolic Cosine";
		}
	},
	/** Inverse Hyperbolic Tangent. */
	atanh {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Inverse Hyperbolic Tangent"
					+ "\nReturns a complex value (Real,Imaginary) use the re() or im() functions to get the corresponding value";
		}
	},
	/** maximum in argument list. */
	max {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Maximum in argument list" + getMultipleString();
		}
	},
	/** minimum in argument list. */
	min {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Minimum in argument list" + getMultipleString();
		}
	},
	/** average in argument list. */
	average {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Average in argument list" + getMultipleString();
		}
	},
	stdev {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Standard Deviation in argument list" + getMultipleString();
		}
	},
	med {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Median in argument list" + getMultipleString();
		}
	},
	shift {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Shift " + getMultipleString();
		}

		@Override
		public int getNrArgs() {
			return 2;
		}
	},
	var {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Variance in argument list" + getMultipleString();
		}

	},
	mean {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Mean in argument list" + getMultipleString();
		}

	},
	count {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Counts the amount of numbers in argument list"
					+ getMultipleString();
		}
	},
	sum {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Sum of argument list" + getMultipleString();
		}
	},
	mad {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Median absolute deviation of argument list"
					+ getMultipleString();
		}
	},
	filter {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Filter: filters the values out of an vector acording to the condition given"
					+ "\nfilter(cond, values)";
		}

		@Override
		public int getNrArgs() {
			return 2;
		}
	}, 
	offset {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Add offset to each argument in the list"
					+ "\noffset(list, offset)";
		}

		@Override
		public int getNrArgs() {
			return 2;
		}
	}, 
	offsetrnd {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Add a random offset to each argument in the list"
					+ "\noffsetRnd(list, offsetBase, offsetMultiplier)";
		}

		@Override
		public int getNrArgs() {
			return 3;
		}
	},
	drop {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Drop the specified amount of values from the list. Positive drop value will start at 0, negative at end."
					+ "\noffset(list, drop)";
		}

		@Override
		public int getNrArgs() {
			return 2;
		}
	},
	cstop {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Give a number or list to the first two parameters and a valid operation as a third parameter (+, -, /, * and ^)"
					+ "\noffset(p1, p2, operation)";
		}
		
		@Override
		public int getNrArgs() {
			return 3;
		}
	}, 
	logicle {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Perform logicle transformation"
					+ "\nlogicle(list, T_TopOfScale*, W_NumberOfDecades*, M_LinearDecades*, A_AddNegDecades*) (* are optional)";
		}
	},
	is_nan {
		/** {@inheritDoc} */
		@Override
		String getDescription() {
			return "Returns 1 if the argument is NaN, 0 otherwise.";
		}

		@Override
		public int getNrArgs() {
			return 1;
		}
	};

	/**
	 * Number of arguments for this function (just used in dialog).
	 * 
	 * @return Number of default arguments.
	 */
	public int getNrArgs() {
		return 1;
	}

	/** @return The function short name. */
	public String getFunctionName() {
		return toString();
	}

	/** @return The function full name (incl. arguments) */
	String getFunctionFullName() {
		StringBuilder b = new StringBuilder(getFunctionName());
		b.append('(');

		for (int i = 0; i < getNrArgs(); i++) {
			b.append(i > 0 ? ", " : "");
			b.append((char) ('x' + i));
		}
		b.append(')');
		return b.toString();
	}

	/** @return short description for function (tooltip). */
	abstract String getDescription();

	/** @return function(x,y,z,..) also allowed */
	String getMultipleString() {
		StringBuilder b = new StringBuilder("\n" + getFunctionName());
		b.append('(');
		for (int i = 0; i < 3; i++) {
			b.append(i > 0 ? ", " : "");
			b.append((char) ('x' + i));
		}
		b.append(",...) also allowed");
		return b.toString();
	}
}
