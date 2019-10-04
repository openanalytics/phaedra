package eu.openanalytics.phaedra.calculation.hitcall.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

@Entity
@Table(name="hca_hit_call_ruleset", schema="phaedra")
@SequenceGenerator(name="hca_hit_call_ruleset_s", sequenceName="hca_hit_call_ruleset_s", schema="phaedra", allocationSize=1)
public class HitCallRuleset implements IValueObject, IOwnedObject {

	@Id
	@Column(name="ruleset_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_hit_call_ruleset_s")
	private long id;
	
	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="protocolclass_id")
	private ProtocolClass protocolClass;
	
	@IgnoreSizeOf
	@OneToMany(mappedBy="ruleset", fetch=FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval=true)
	@OrderColumn(name="ruleset_sequence")
	private List<HitCallRule> rules;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public ProtocolClass getProtocolClass() {
		return protocolClass;
	}
	public void setProtocolClass(ProtocolClass protocolClass) {
		this.protocolClass = protocolClass;
	}
	public List<HitCallRule> getRules() {
		return rules;
	}
	public void setRules(List<HitCallRule> rules) {
		this.rules = rules;
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
		HitCallRuleset other = (HitCallRuleset) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("Ruleset %d [pclass %s] [%d rules]", id, protocolClass, (rules == null) ? 0 : rules.size());
	}
}
