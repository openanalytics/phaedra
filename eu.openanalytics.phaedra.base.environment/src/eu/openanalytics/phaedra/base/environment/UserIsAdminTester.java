package eu.openanalytics.phaedra.base.environment;

import org.eclipse.core.expressions.PropertyTester;

import eu.openanalytics.phaedra.base.security.SecurityService;

public class UserIsAdminTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		return SecurityService.getInstance().isGlobalAdmin();
	}

}
