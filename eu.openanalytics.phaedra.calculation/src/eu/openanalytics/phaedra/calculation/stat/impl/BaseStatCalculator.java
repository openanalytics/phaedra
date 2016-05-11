package eu.openanalytics.phaedra.calculation.stat.impl;

import eu.openanalytics.phaedra.calculation.stat.IStatCalculator;
import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

public abstract class BaseStatCalculator implements IStatCalculator {

	private String name;
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public abstract double calculate(IStatContext context);

}
