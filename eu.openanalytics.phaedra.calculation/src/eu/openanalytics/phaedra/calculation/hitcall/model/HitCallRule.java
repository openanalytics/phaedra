package eu.openanalytics.phaedra.calculation.hitcall.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

@Entity
@Table(name="hca_hit_call_rule", schema="phaedra")
@SequenceGenerator(name="hca_hit_call_rule_s", sequenceName="hca_hit_call_rule_s", schema="phaedra", allocationSize=1)
public class HitCallRule implements IValueObject, IOwnedObject {

	@Id
	@Column(name="rule_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_hit_call_rule_s")
	private long id;
	
	@Column(name="rule_name")
	private String name;
	
	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="formula_id")
	private CalculationFormula formula;
	
	@Column(name="threshold")
	private double threshold;
	
	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="protocolclass_id")
	private ProtocolClass protocolClass;
	
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
	public CalculationFormula getFormula() {
		return formula;
	}
	public void setFormula(CalculationFormula formula) {
		this.formula = formula;
	}
	public double getThreshold() {
		return threshold;
	}
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	public ProtocolClass getProtocolClass() {
		return protocolClass;
	}
	public void setProtocolClass(ProtocolClass protocolClass) {
		this.protocolClass = protocolClass;
	}
	
	@Override
	public String[] getOwners() {
		ProtocolClass pClass = getProtocolClass();
		if (pClass != null) return pClass.getOwners();
		return new String[0];
	}

	@Override
	public IValueObject getParent() {
		return getProtocolClass();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HitCallRule other = (HitCallRule) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("%s [%d]", name, id);
	}
	
}
