package eu.openanalytics.phaedra.base.search.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;
import org.springframework.beans.BeanUtils;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.db.jpa.converter.ClassTypeConverter;
import eu.openanalytics.phaedra.base.search.Activator;
import eu.openanalytics.phaedra.base.search.preferences.Prefs;

@Entity
@Table(name="query", schema="phaedra")
@SequenceGenerator(name="query_s", sequenceName="query_s", schema="phaedra", allocationSize=1)
@Converter(name="class-convertor", converterClass=ClassTypeConverter.class)
@NamedQueries({
	@NamedQuery(name=QueryModel.NAMED_QUERY_GET_PUBLIC_QUERIES, query="select q from QueryModel q where q.publicQuery = true and q.example = false"),
	@NamedQuery(name=QueryModel.NAMED_QUERY_GET_MY_QUERIES, query="select q from QueryModel q where q.publicQuery = false and q.example = false and q.owner = :owner"),
	@NamedQuery(name=QueryModel.NAMED_QUERY_GET_EXAMPLE_QUERIES, query="select q from QueryModel q where q.example = true"),
	@NamedQuery(name=QueryModel.NAMED_QUERY_GET_SIMILAR_QUERIES, query="select q from QueryModel q where lower(:name) = lower(q.name) and (q.publicQuery = true or (q.publicQuery = false and q.owner = :owner))"),
})
public class QueryModel implements Serializable, IValueObject {
	private static final long serialVersionUID = -3703305916188490237L;

	private static final boolean DEFAULT_MAX_RESULTS_SET = true;
	private static final boolean DEFAULT_PUBLIC_QUERY = false;
	private static final boolean DEFAULT_EXAMPLE = false;

	public static final String NAMED_QUERY_GET_PUBLIC_QUERIES = "getPublicQueries";
	public static final String NAMED_QUERY_GET_MY_QUERIES = "getMyQueries";
	public static final String NAMED_QUERY_GET_EXAMPLE_QUERIES = "getExampleQueries";
	public static final String NAMED_QUERY_GET_SIMILAR_QUERIES = "getSimilarQueries";

	@Id
	@Column(name="query_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="query_s")
	private long id;

	@Column(name="query_name")
	@Size(max=100)
	private String name;
	@Column(name="description")
	@Size(max=255)
	private String description;
	@Column(name="remark")
	@Size(max=255)
	private String remark;

	@Column(name="query_user")
	private String owner;
	@Temporal(TemporalType.DATE)
	@Column(name="query_dt")
	private Date date;
	@Column(name="is_public")
	private boolean publicQuery = DEFAULT_PUBLIC_QUERY;
	@Column(name="example")
	private boolean example = DEFAULT_EXAMPLE;

	@Convert("class-convertor")
	@Column(name="type")
	private Class<? extends PlatformObject> type;
	@Column(name="max_results_set")
	private boolean maxResultsSet = DEFAULT_MAX_RESULTS_SET;
	@Column(name="max_results")
	private int maxResults = getDefaultMaxResults();

	@OneToMany(mappedBy="queryModel", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<QueryFilter> queryFilters = new LinkedList<>();
	@OneToMany(mappedBy="queryModel", cascade=CascadeType.ALL, orphanRemoval=true)
	@OrderColumn(name="ordering_index")
	private List<QueryOrdering> queryOrderings = new LinkedList<>();

	public QueryModel() {
	}

	/*
	 * *****************
	 * Getters & setters
	 * *****************
	 */

	@Override
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public boolean isPublicQuery() {
		return publicQuery;
	}

	public void setPublicQuery(boolean publicQuery) {
		this.publicQuery = publicQuery;
	}

	public boolean isExample() {
		return example;
	}

	public void setExample(boolean example) {
		this.example = example;
	}

	public Class<? extends PlatformObject> getType() {
		return type;
	}

	public void setType(Class<? extends PlatformObject> type) {
		this.type = type;
	}

	public boolean isMaxResultsSet() {
		return maxResultsSet;
	}

	public void setMaxResultsSet(boolean maxResultsSet) {
		this.maxResultsSet = maxResultsSet;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	public List<QueryFilter> getQueryFilters() {
		return queryFilters;
	}

	public void setQueryFilters(List<QueryFilter> queryFilters) {
		this.queryFilters = queryFilters;
	}

	public List<QueryOrdering> getQueryOrderings() {
		return queryOrderings;
	}

	public void setQueryOrderings(List<QueryOrdering> queryOrderings) {
		this.queryOrderings = queryOrderings;
	}


	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

	@Override
	public int hashCode() {
		if (id == 0) {
			return super.hashCode();
		}
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (id == 0) {
			return super.equals(obj);
		}
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryModel other = (QueryModel) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name + " (" + id + ")";
//		return "QueryModel [id=" + id + ", name=" + name + ", description="
//				+ description + ", remark=" + remark + ", owner=" + owner
//				+ ", date=" + date + ", publicQuery=" + publicQuery
//				+ ", example=" + example + ", type=" + type
//				+ ", maxResultsSet=" + maxResultsSet + ", maxResults="
//				+ maxResults + ", queryFilters=" + queryFilters
//				+ ", queryOrderings=" + queryOrderings + "]";
	}


	/*
	 * *******************
	 * Extra methods
	 * *******************
	 */

	public void addQueryFilter(QueryFilter queryFilter) {
		queryFilters.add(queryFilter);
		queryFilter.setQueryModel(this);
	}

	public void removeQueryFilter(QueryFilter queryFilter) {
		queryFilters.remove(queryFilter);
	}

	public void clearQueryFilters() {
		queryFilters.clear();
	}

	public void addQueryOrdering(QueryOrdering queryOrdering) {
		queryOrderings.add(queryOrdering);
		queryOrdering.setQueryModel(this);
	}

	public void removeQueryOrdering(QueryOrdering queryOrdering) {
		queryOrderings.remove(queryOrdering);
	}

	public void clearQueryOrderings() {
		queryOrderings.clear();
	}

	public void moveUpQueryOrdering(String columnName) {
		int index = QueryOrdering.indexOfQueryOrdering(queryOrderings, columnName);
		if (index > 0) {
			Collections.swap(queryOrderings, index, index - 1);
		}
	}

	public void moveDownQueryOrdering(String columnName) {
		int index = QueryOrdering.indexOfQueryOrdering(queryOrderings, columnName);
		if (index >= 0 && index < queryOrderings.size() - 1) {
			Collections.swap(queryOrderings, index, index + 1);
		}
	}

	public boolean canMoveUpQueryOrdering(int index) {
		return index > 0;
	}

	public boolean canMoveDownQueryOrdering(int index) {
		return index >= 0 && index < queryOrderings.size() - 1;
	}

	public static int getDefaultMaxResults() {
		return Activator.getDefault().getPreferenceStore().getInt(Prefs.DEFAULT_MAX_RESULTS);
	}

	@Override
	public IValueObject getParent() {
		return null;
	}

	@Transient
	public QueryModel getCopy() {
		QueryModel newQueryModel = new QueryModel();
		BeanUtils.copyProperties(this, newQueryModel, new String[] {"id", "name", "date", "owner", "queryFilters", "queryOrderings"});
		for (QueryFilter filter : queryFilters) {
			QueryFilter newFilter = new QueryFilter();
			BeanUtils.copyProperties(filter, newFilter, new String[] {"id", "queryModel"});
			newQueryModel.addQueryFilter(newFilter);
		}
		for (QueryOrdering ordering : queryOrderings) {
			QueryOrdering newOrdering = new QueryOrdering();
			BeanUtils.copyProperties(ordering, newOrdering, new String[] {"id", "queryModel"});
			newQueryModel.addQueryOrdering(newOrdering);
		}
		return newQueryModel;
	}

	/**
	 * Merge the values of the other model into this model (not overwriting ids).
	 */
	@Transient
	public void merge(QueryModel other) {
		BeanUtils.copyProperties(other, this, new String[] {"id", "queryFilters", "queryOrderings"});

		// Filters
		if (queryFilters == null) queryFilters = new ArrayList<>();
		int myFilters = queryFilters.size();
		int otherFilters = other.queryFilters.size();

		if (myFilters > otherFilters) {
			List<QueryFilter> filtersToRemove = new ArrayList<>();
			for (int i=otherFilters; i<myFilters; i++) filtersToRemove.add(queryFilters.get(i));
			queryFilters.removeAll(filtersToRemove);
		}
		for (int i=0; i<Math.min(myFilters, otherFilters); i++) {
			BeanUtils.copyProperties(other.queryFilters.get(i), queryFilters.get(i), new String[] {"id", "queryModel"});
		}
		for (int i=myFilters; i<otherFilters; i++) {
			queryFilters.add(other.queryFilters.get(i));
			other.queryFilters.get(i).setQueryModel(this);
		}

		// Orderings
		if (queryOrderings == null) queryOrderings = new ArrayList<>();
		int myOrderings = queryOrderings.size();
		int otherOrderings = other.queryOrderings.size();

		if (myOrderings > otherOrderings) {
			List<QueryOrdering> orderingsToRemove = new ArrayList<>();
			for (int i=otherOrderings; i<myOrderings; i++) orderingsToRemove.add(queryOrderings.get(i));
			queryOrderings.removeAll(orderingsToRemove);
		}
		for (int i=0; i<Math.min(myOrderings, otherOrderings); i++) {
			BeanUtils.copyProperties(other.queryOrderings.get(i), queryOrderings.get(i), new String[] {"id", "queryModel"});
		}
		for (int i=myOrderings; i<otherOrderings; i++) {
			queryOrderings.add(other.queryOrderings.get(i));
			other.queryOrderings.get(i).setQueryModel(this);
		}
	}
}
