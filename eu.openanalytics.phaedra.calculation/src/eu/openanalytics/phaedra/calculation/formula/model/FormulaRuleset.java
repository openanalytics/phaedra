package eu.openanalytics.phaedra.calculation.formula.model;

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

import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

@Entity
@Table(name="hca_formula_ruleset", schema="phaedra")
@SequenceGenerator(name="hca_formula_ruleset_s", sequenceName="hca_formula_ruleset_s", schema="phaedra", allocationSize=1)
public class FormulaRuleset implements IValueObject, IOwnedObject {

	@Id
	@Column(name="ruleset_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_formula_ruleset_s")
	private long id;
	
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="feature_id")
	private Feature feature;
	
	@IgnoreSizeOf
	@BatchFetch(BatchFetchType.JOIN)
	@OneToMany(mappedBy="ruleset", fetch=FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval=true)
	@OrderColumn(name="ruleset_sequence")
	private List<FormulaRule> rules;

	@Column(name="type")
	private int type;
	
	@Column(name="show_in_ui")
	private boolean showInUI;
	
	@Column(name="color")
	private int color;
	
	@Column(name="style")
	private int style;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public Feature getFeature() {
		return feature;
	}
	public void setFeature(Feature feature) {
		this.feature = feature;
	}
	public List<FormulaRule> getRules() {
		return rules;
	}
	public void setRules(List<FormulaRule> rules) {
		this.rules = rules;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public boolean isShowInUI() {
		return showInUI;
	}
	public void setShowInUI(boolean showInUI) {
		this.showInUI = showInUI;
	}
	public int getColor() {
		return color;
	}
	public void setColor(int color) {
		this.color = color;
	}
	public int getStyle() {
		return style;
	}
	public void setStyle(int style) {
		this.style = style;
	}
	
	@Override
	public String[] getOwners() {
		Feature feature = getFeature();
		if (feature != null) return feature.getOwners();
		return new String[0];
	}

	@Override
	public IValueObject getParent() {
		return getFeature();
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
		FormulaRuleset other = (FormulaRuleset) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("Ruleset %d [feature %s] [%d rules]", id, feature, (rules == null) ? 0 : rules.size());
	}
}
