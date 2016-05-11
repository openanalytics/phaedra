package eu.openanalytics.phaedra.base.search.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.db.jpa.converter.ClassTypeConverter;

@Entity
@Table(name="query_ordering", schema="phaedra")
@SequenceGenerator(name="query_ordering_s", sequenceName="query_ordering_s", schema="phaedra", allocationSize=1)
@Converter(name="class-convertor", converterClass=ClassTypeConverter.class)
public class QueryOrdering {
	private static final boolean DEFAULT_ASCENDING = true;
	private static final boolean DEFAULT_CASE_SENSITIVE = false;
	
	@Id
	@Column(name="query_ordering_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="query_ordering_s")
	private long id;
	
	@Column(name="column_name")
	private String columnName;
		
	@Convert("class-convertor")
	@Column(name="column_type")
	private Class<?> columnType;
	
	@Column(name="ascending")
	private boolean ascending = DEFAULT_ASCENDING;
	
	@Column(name="case_sensitive")
	private boolean caseSensitive = DEFAULT_CASE_SENSITIVE;

	@Column(name="ordering_index")
	private int orderingIndex;
	
	@ManyToOne
	@JoinColumn(name="query_id")
	private QueryModel queryModel;
		
	public QueryOrdering() {
	}
	
	public QueryOrdering(String columnName, Class<?> columnType, Boolean ascending, Boolean caseSensitive) {
		this.columnName = columnName;
		this.columnType = columnType;
		this.ascending = ascending;
		this.caseSensitive = caseSensitive;
	}
	
	/*
	 * *****************
	 * Getters & setters
	 * *****************
	 */
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	public Class<?> getColumnType() {
		return columnType;
	}

	public void setColumnType(Class<?> columnType) {
		this.columnType = columnType;
	}

	public boolean isAscending() {
		return ascending;
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}
	
	public int getOrderingIndex() {
		return orderingIndex;
	}

	public void setOrderingIndex(int orderingIndex) {
		this.orderingIndex = orderingIndex;
	}

	public QueryModel getQueryModel() {
		return queryModel;
	}
	
	public void setQueryModel(QueryModel queryModel) {
		this.queryModel = queryModel;
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
		QueryOrdering other = (QueryOrdering) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "QueryOrdering [columnName=" + columnName + ", columnType="
				+ columnType + ", ascending=" + ascending + ", caseSensitive="
				+ caseSensitive + "]";
	}
	
	
	/*
	 * *******************
	 * Extra methods
	 * *******************
	 */
	
	public static int indexOfQueryOrdering(List<QueryOrdering> queryOrderings, final String columnName) {
		return Iterables.indexOf(queryOrderings, new Predicate<QueryOrdering>() {
			@Override
			public boolean apply(QueryOrdering queryOrdering) {
				return columnName.equals(queryOrdering.getColumnName());
			}
		});
	}
	
	public static List<String> getColumnNames(List<QueryOrdering> queryOrderings) {
		return Lists.transform(queryOrderings, new Function<QueryOrdering, String>() {
			@Override
			public String apply(QueryOrdering queryOrdering) {
				return queryOrdering.getColumnName();
			}
		});
	}

}
