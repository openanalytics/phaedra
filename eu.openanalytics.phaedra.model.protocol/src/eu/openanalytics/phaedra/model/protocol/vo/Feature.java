package eu.openanalytics.phaedra.model.protocol.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;

@Entity
@Table(name="hca_feature", schema="phaedra")
@SequenceGenerator(name="hca_feature_s", sequenceName="hca_feature_s", schema="phaedra", allocationSize=1)
public class Feature extends PlatformObject implements IFeature, Serializable {

	private static final long serialVersionUID = -8316009750892994206L;

	@Id
	@Column(name="feature_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_feature_s")
	private long id;

	@Column(name="feature_name")
	private String name;
	@Column(name="short_name")
	private String shortName;
	@Column(name="description")
	private String description;
	
	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="protocolclass_id")
	private ProtocolClass protocolClass;

	@IgnoreSizeOf
	@BatchFetch(BatchFetchType.JOIN)
	@OneToMany(mappedBy="wellFeature", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<FeatureClass> featureClasses;

	@Column(name="is_classification_restricted")
	private boolean classificationRestricted;
	
	@Column(name="is_numeric")
	private boolean numeric;
	@Column(name="is_logarithmic")
	private boolean logarithmic;
	@Column(name="is_required")
	private boolean required;
	@Column(name="is_key")
	private boolean key;
	@Column(name="is_uploaded")
	private boolean uploaded;
	@Column(name="is_annotation")
	private boolean annotation;

	@Column(name="calc_formula")
	private String calculationFormula;
	@Column(name="calc_formula_id")
	private long calculationFormulaId;
	@Column(name="calc_language")
	private String calculationLanguage;
	@Column(name="calc_trigger")
	private String calculationTrigger;
	@Column(name="calc_sequence")
	private int calculationSequence;

	@Column(name="format_string")
	private String formatString;

	@Column(name="low_welltype")
	private String lowWellTypeCode;
	@Column(name="high_welltype")
	private String highWellTypeCode;

	@IgnoreSizeOf
	@ElementCollection
	@BatchFetch(BatchFetchType.JOIN)
	@MapKeyColumn(name="setting_name")
	@Column(name="setting_value")
	@CollectionTable(name="hca_curve_setting", schema="phaedra", joinColumns=@JoinColumn(name="feature_id"))
	private Map<String,String> curveSettings;

	@Column(name="curve_normalization")
	private String normalization;
	@Column(name="normalization_language")
	private String normalizationLanguage;
	@Column(name="normalization_formula")
	private String normalizationFormula;
	@Column(name="normalization_scope")
	private int normalizationScope;

	@IgnoreSizeOf
	@ElementCollection
	@BatchFetch(BatchFetchType.JOIN)
	@MapKeyColumn(name="setting_name")
	@Column(name="setting_value")
	@CollectionTable(name="hca_colormethod_setting", schema="phaedra", joinColumns=@JoinColumn(name="feature_id"))
	private Map<String,String> colorMethodSettings;

	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.OUTER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="group_id")
	private FeatureGroup featureGroup;

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

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public boolean isCalculated() {
		return calculationFormulaId > 0 || (calculationFormula != null && !calculationFormula.isEmpty());
	}

	public String getCalculationFormula() {
		return calculationFormula;
	}

	public void setCalculationFormula(String calculationFormula) {
		this.calculationFormula = calculationFormula;
	}

	public long getCalculationFormulaId() {
		return calculationFormulaId;
	}
	
	public void setCalculationFormulaId(long calculationFormulaId) {
		this.calculationFormulaId = calculationFormulaId;
	}
	
	public String getCalculationLanguage() {
		return calculationLanguage;
	}

	public void setCalculationLanguage(String calculationLanguage) {
		this.calculationLanguage = calculationLanguage;
	}

	public String getCalculationTrigger() {
		return calculationTrigger;
	}

	public int getCalculationSequence() {
		return calculationSequence;
	}

	public void setCalculationSequence(int calculationSequence) {
		this.calculationSequence = calculationSequence;
	}

	public void setCalculationTrigger(String calculationTrigger) {
		this.calculationTrigger = calculationTrigger;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getFormatString() {
		return formatString;
	}

	public void setFormatString(String formatString) {
		this.formatString = formatString;
	}

	public String getLowWellTypeCode() {
		return lowWellTypeCode;
	}

	public void setLowWellTypeCode(String lowWellTypeCode) {
		this.lowWellTypeCode = lowWellTypeCode;
	}

	public String getHighWellTypeCode() {
		return highWellTypeCode;
	}

	public void setHighWellTypeCode(String highWellTypeCode) {
		this.highWellTypeCode = highWellTypeCode;
	}

	@Override
	public ProtocolClass getProtocolClass() {
		return protocolClass;
	}

	public void setProtocolClass(ProtocolClass protocolClass) {
		this.protocolClass = protocolClass;
	}

	@Override
	public List<FeatureClass> getFeatureClasses() {
		return featureClasses;
	}

	public void setFeatureClasses(List<FeatureClass> featureClasses) {
		this.featureClasses = featureClasses;
	}

	public boolean isClassificationRestricted() {
		return classificationRestricted;
	}
	
	public void setClassificationRestricted(boolean classificationRestricted) {
		this.classificationRestricted = classificationRestricted;
	}
	
	public Map<String, String> getCurveSettings() {
		return curveSettings;
	}

	public void setCurveSettings(Map<String, String> curveSettings) {
		this.curveSettings = curveSettings;
	}

	public String getNormalization() {
		return normalization;
	}

	public void setNormalization(String normalization) {
		this.normalization = normalization;
	}

	public String getNormalizationLanguage() {
		return normalizationLanguage;
	}

	public void setNormalizationLanguage(String normalizationLanguage) {
		this.normalizationLanguage = normalizationLanguage;
	}

	public String getNormalizationFormula() {
		return normalizationFormula;
	}

	public void setNormalizationFormula(String normalizationFormula) {
		this.normalizationFormula = normalizationFormula;
	}

	public int getNormalizationScope() {
		return normalizationScope;
	}

	public void setNormalizationScope(int normalizationScope) {
		this.normalizationScope = normalizationScope;
	}

	public void setNumeric(boolean numeric) {
		this.numeric = numeric;
	}

	@Override
	public boolean isNumeric() {
		return numeric;
	}

	public void setLogarithmic(boolean logarithmic) {
		this.logarithmic = logarithmic;
	}

	public boolean isLogarithmic() {
		return logarithmic;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isRequired() {
		return required;
	}

	public void setKey(boolean key) {
		this.key = key;
	}

	@Override
	public boolean isKey() {
		return key;
	}

	public void setUploaded(boolean uploaded) {
		this.uploaded = uploaded;
	}

	public boolean isUploaded() {
		return uploaded;
	}

	public void setAnnotation(boolean annotation) {
		this.annotation = annotation;
	}
	
	public boolean isAnnotation() {
		return annotation;
	}
	
	public Map<String, String> getColorMethodSettings() {
		return colorMethodSettings;
	}

	public void setColorMethodSettings(Map<String, String> colorMethodSettings) {
		this.colorMethodSettings = colorMethodSettings;
	}

	@Override
	public FeatureGroup getFeatureGroup() {
		return featureGroup;
	}

	public void setFeatureGroup(FeatureGroup featureGroup) {
		this.featureGroup = featureGroup;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

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
		Feature other = (Feature) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name + " (" + id + ")";
	}

	@Override
	public String getDisplayName() {
		if (shortName != null && !shortName.isEmpty()) {
			return shortName;
		}
		return name;
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
}
