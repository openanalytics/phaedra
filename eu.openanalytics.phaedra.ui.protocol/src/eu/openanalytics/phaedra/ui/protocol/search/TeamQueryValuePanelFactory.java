package eu.openanalytics.phaedra.ui.protocol.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.search.EnumeratedTextQueryValuePanelFactory;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;


public class TeamQueryValuePanelFactory extends EnumeratedTextQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(Protocol.class, "teamCode", OperatorType.STRING, Operator.STRING_EQUALS, null))));
	
	private static List<String> TEAMS = new ArrayList<>(SecurityService.getInstance().getAllTeams());
	
	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}
	
	@Override
	public List<String> getEnumeration() {
		return TEAMS;
	}
	
}
	