package eu.openanalytics.phaedra.base.ui.search.navigator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.PlatformObject;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import eu.openanalytics.phaedra.base.search.SearchService;
import eu.openanalytics.phaedra.base.search.model.QueryModel;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.NavigatorContentProvider;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.Group;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;
import eu.openanalytics.phaedra.base.ui.search.internal.QueryEditorSupportRegistry;

public class QueryProvider implements IElementProvider {
	protected static final String SAVED_QUERIES = "savedQueries";
	protected static final String PUBLIC_QUERIES = "publicQueries";
	protected static final String MY_QUERIES = "myQueries";
	protected static final String EXAMPLE_QUERIES = "exampleQueries";
		
	@Override
	public IElement[] getChildren(final IGroup parent) {
		if (Objects.equal(parent, NavigatorContentProvider.ROOT_GROUP)) {
			return new IElement[] {new Group("Queries", SAVED_QUERIES, parent.getId())};
		}
		if (Objects.equal(parent.getId(), SAVED_QUERIES)) {
			Group myQueriesGroup = new Group("My Queries", MY_QUERIES, SAVED_QUERIES);
			myQueriesGroup.setImageDescriptor(IconManager.getIconDescriptor("user.png"));
			Group publicQueriesGroup = new Group("Public Queries", PUBLIC_QUERIES, SAVED_QUERIES);
			publicQueriesGroup.setImageDescriptor(IconManager.getIconDescriptor("group.png"));
			Group exampleQueriesGroup = new Group("Example Queries", EXAMPLE_QUERIES, SAVED_QUERIES);
			exampleQueriesGroup.setImageDescriptor(IconManager.getIconDescriptor("user_comment.png"));
			return new IElement[] {myQueriesGroup, publicQueriesGroup, exampleQueriesGroup};
		}
		if (parent.getId().startsWith(MY_QUERIES)) {
			List<QueryModel> myQueries = SearchService.getInstance().getMyQueries();
			if (Objects.equal(parent.getId(), MY_QUERIES)) {
				return getGroups(getQueryTypes(myQueries), MY_QUERIES);
			}
			for (Class<? extends PlatformObject> type : getQueryTypes(myQueries)) {
				if (Objects.equal(parent.getId(), MY_QUERIES + "-" + type.getSimpleName())) {
					return getElements(getQueryModelsByType(myQueries, type), MY_QUERIES);
				}
			}
		}
		if (parent.getId().startsWith(PUBLIC_QUERIES)) {
			List<QueryModel> publicQueries = SearchService.getInstance().getPublicQueries();
			if (Objects.equal(parent.getId(), PUBLIC_QUERIES)) {
				return getGroups(getQueryTypes(publicQueries), PUBLIC_QUERIES);
			}
			for (Class<? extends PlatformObject> type : getQueryTypes(publicQueries)) {
				if (Objects.equal(parent.getId(), PUBLIC_QUERIES + "-" + type.getSimpleName())) {
					return getElements(getQueryModelsByType(publicQueries, type), PUBLIC_QUERIES);
				}
			}
		}
		if (parent.getId().startsWith(EXAMPLE_QUERIES)) {
			List<QueryModel> exampleQueries = SearchService.getInstance().getExampleQueries();
			if (Objects.equal(parent.getId(), EXAMPLE_QUERIES)) {
				return getGroups(getQueryTypes(exampleQueries), EXAMPLE_QUERIES);
			}
			for (Class<? extends PlatformObject> type : getQueryTypes(exampleQueries)) {
				if (Objects.equal(parent.getId(), EXAMPLE_QUERIES + "-" + type.getSimpleName())) {
					return getElements(getQueryModelsByType(exampleQueries, type), EXAMPLE_QUERIES);
				}
			}
		}
		return null;
	}

	private Set<Class<? extends PlatformObject>> getQueryTypes(List<QueryModel> queryModels) {
		return new HashSet<>(Collections2.transform(queryModels, new Function<QueryModel, Class<? extends PlatformObject>>() {
			@Override
			public Class<? extends PlatformObject> apply(QueryModel queryModel) {
				return queryModel.getType();
			}
		}));
	}
	
	private List<QueryModel> getQueryModelsByType(List<QueryModel> queryModels, final Class<? extends PlatformObject> type) {
		return new ArrayList<>(Collections2.filter(queryModels, new Predicate<QueryModel>() {
			@Override
			public boolean apply(QueryModel queryModel) {
				return Objects.equal(queryModel.getType(), type);
			}
		}));
	}
	
	private IElement[] getGroups(Set<Class<? extends PlatformObject>> types, final String parentId) {
		List<IElement> elements = new ArrayList<>(Collections2.transform(types,new Function<Class<? extends PlatformObject>, IElement>() {
			@Override
			public IElement apply(Class<? extends PlatformObject> type) {
				Group group = new Group(QueryEditorSupportRegistry.getInstance().getFactory(type).getLabel(), parentId + "-" + type.getSimpleName(), parentId);
				group.setImageDescriptor(IconManager.getDefaultIconDescriptor(type));
				return group;
			}
		}));
		return elements.toArray(new IElement[0]);
	}
	
	private IElement[] getElements(List<QueryModel> queryModels, final String parentId) {
		List<IElement> elements = new ArrayList<>(Collections2.transform(queryModels, new Function<QueryModel, IElement>() {
			@Override
			public IElement apply(QueryModel queryModel) {
				Element element = new Element(queryModel.getName(), parentId + "-" + queryModel.getId(), parentId);
				element.setTooltip(queryModel.getDescription());
				element.setData(queryModel);
				element.setImageDescriptor(IconManager.getDefaultIconDescriptor(queryModel.getType()));
				return element;
			}
		}));
		return elements.toArray(new IElement[0]);
	}

}
