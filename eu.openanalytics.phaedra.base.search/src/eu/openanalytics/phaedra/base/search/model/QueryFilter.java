package eu.openanalytics.phaedra.base.search.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

import eu.openanalytics.phaedra.base.db.jpa.converter.ClassTypeConverter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;

@Entity
@Table(name="query_filter", schema="phaedra")
@SequenceGenerator(name="query_filter_s", sequenceName="query_filter_s", schema="phaedra", allocationSize=1)
@Converter(name="class-convertor", converterClass=ClassTypeConverter.class)
public class QueryFilter implements Serializable {
	private static final long serialVersionUID = -3831490166709712145L;

	private static final boolean DEFAULT_POSITIVE = true;
	private static final boolean DEFAULT_CASE_SENSITIVE = false; 
	
	@Id
	@Column(name="query_filter_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="query_filter_s")
	private long id;
	
	@Convert("class-convertor")
	@Column(name="type")
	private Class<? extends PlatformObject> type;
	
	@Column(name="column_name")
	private String columnName;
	
	@Column(name="positive")
	private boolean positive = DEFAULT_POSITIVE;
	
	@Enumerated(EnumType.STRING)
	@Column(name="operator_type")
	private OperatorType operatorType;
	
	@Enumerated(EnumType.STRING)
	@Column(name="operator")
	private Operator operator;
	
	@Column(name="case_sensitive")
	private boolean caseSensitive = DEFAULT_CASE_SENSITIVE;
	
	@Lob
	@Column(name="value")
	private Serializable value;
	
	@ManyToOne
	@JoinColumn(name="query_id")
	private QueryModel queryModel;
	
	public QueryFilter() {
	}
	
	public QueryFilter(Class<? extends PlatformObject> type, String columnName, Boolean positive, OperatorType operatorType, Operator operator, Boolean caseInsensitive, Serializable value) {
		this.type = type;
		this.columnName = columnName;
		this.positive = positive;
		this.operatorType = operatorType;
		this.operator = operator;
		this.caseSensitive = caseInsensitive;
		this.value = value;
	}

	public QueryFilter(Class<? extends PlatformObject> type, String columnName, OperatorType operatorType, Operator operator, Serializable value) {
		this(type, columnName, DEFAULT_POSITIVE, operatorType, operator, DEFAULT_CASE_SENSITIVE, value);
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

	public Class<? extends PlatformObject> getType() {
		return type;
	}

	public void setType(Class<? extends PlatformObject> type) {
		this.type = type;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
 
	public boolean isPositive() {
		return positive;
	}

	public void setPositive(boolean positive) {
		this.positive = positive;
	}

	public OperatorType getOperatorType() {
		return operatorType;
	}

	public void setOperatorType(OperatorType operatorType) {
		this.operatorType = operatorType;
	}

	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}
	
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public Serializable getValue() {
		return value;
	}
		
	public void setValue(Serializable value) {
		this.value = value;
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
		QueryFilter other = (QueryFilter) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "QueryFilter [type=" + type + ", columnName=" + columnName
				+ ", positive=" + positive + ", operatorType=" + operatorType
				+ ", operator=" + operator + ", caseSensitive=" + caseSensitive
				+ ", value=" + value + "]";
	}

}
