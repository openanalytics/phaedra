package eu.openanalytics.phaedra.model.protocol.vo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.db.IValueObject;

@Entity
@Table(name="hca_classification", schema="phaedra")
@SequenceGenerator(name="hca_classification_s", sequenceName="hca_classification_s", schema="phaedra", allocationSize=1)
public class FeatureClass extends PlatformObject implements IValueObject, Serializable {

	private static final long serialVersionUID = -7164088071546537485L;

	@Id
	@Column(name="classification_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_classification_s")
	private long id;

	@Column(name="pattern")
	private String pattern;
	@Column(name="pattern_type")
	private String patternType;
	@Column(name="rgb_color")
	private int rgbColor;
	@Column(name="label")
	private String label;
	@Column(name="symbol")
	private String symbol;
	@Column(name="description")
	private String description;

	@ManyToOne
	@JoinColumn(name="subwellfeature_id")
	private SubWellFeature subWellFeature;

	@ManyToOne
	@JoinColumn(name="wellfeature_id")
	private Feature wellFeature;

	public final static String TYPE_BIT = "bit";		// 11...010
	public final static String TYPE_RANGE = "range";	// [1,5000]
	public final static String TYPE_REGEX = "regex";	// $abc.{\\n*}

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

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getPatternType() {
		return patternType;
	}

	public void setPatternType(String patternType) {
		this.patternType = patternType;
	}

	public int getRgbColor() {
		return rgbColor;
	}

	public void setRgbColor(int c) {
		this.rgbColor = c;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public SubWellFeature getSubWellFeature() {
		return subWellFeature;
	}

	public void setSubWellFeature(SubWellFeature subWellFeature) {
		this.subWellFeature = subWellFeature;
	}

	public Feature getWellFeature() {
		return wellFeature;
	}

	public void setWellFeature(Feature wellFeature) {
		this.wellFeature = wellFeature;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

	@Override
	public String toString() {
		return label + " (" + id + ")";
	}

	@Override
	public IValueObject getParent() {
		return getSubWellFeature();
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
		FeatureClass other = (FeatureClass) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
