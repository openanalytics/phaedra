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

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;

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

	@Column(name="ruleset_sequence")
	private int sequence;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="formula_id")
	private CalculationFormula formula;
	
	@Column(name="threshold")
	private double threshold;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="ruleset_id")
	private HitCallRuleset ruleset;
	
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
	public int getSequence() {
		return sequence;
	}
	public void setSequence(int sequence) {
		this.sequence = sequence;
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
	public HitCallRuleset getRuleset() {
		return ruleset;
	}
	public void setRuleset(HitCallRuleset ruleset) {
		this.ruleset = ruleset;
	}
	
	@Override
	public String[] getOwners() {
		return ruleset.getOwners();
	}

	@Override
	public IValueObject getParent() {
		return ruleset.getParent();
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
