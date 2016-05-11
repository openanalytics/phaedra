package eu.openanalytics.phaedra.base.scripting.javascript;

import jdk.nashorn.api.scripting.ClassFilter;

public class EngineClassFilter implements ClassFilter {

	@Override
	public boolean exposeToScripts(String className) {
		//TODO Implement class filter. Currently, all classes are exposed. Note: reflection is disabled.
		return true;
	}

}
