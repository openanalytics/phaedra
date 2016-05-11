package eu.openanalytics.phaedra.ui.plate.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.search.EnumeratedTextQueryValuePanelFactory;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.user.UserService;
import eu.openanalytics.phaedra.model.user.vo.User;


public class UserQueryValuePanelFactory extends EnumeratedTextQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(Experiment.class, "creator", OperatorType.STRING, Operator.STRING_EQUALS, null),
			new QueryFilter(Plate.class, "uploadUser", OperatorType.STRING, Operator.STRING_EQUALS, null),
			new QueryFilter(Plate.class, "approvalUser", OperatorType.STRING, Operator.STRING_EQUALS, null),
			new QueryFilter(Plate.class, "validationUser", OperatorType.STRING, Operator.STRING_EQUALS, null))));
	
	private static List<String> USERS = Lists.transform(UserService.getInstance().getUsers(), new Function<User, String>() {
		public String apply(User user) {
			return user.getUserName().toLowerCase();
		};
	});
	
	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}

	@Override
	public List<String> getEnumeration() {
		return USERS;
	}

	@Override
	public String getDefaultValue() {
		return SecurityService.getInstance().getCurrentUserName();
	}
	
}
	