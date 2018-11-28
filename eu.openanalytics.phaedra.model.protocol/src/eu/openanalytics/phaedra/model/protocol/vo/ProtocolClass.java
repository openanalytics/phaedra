package eu.openanalytics.phaedra.model.protocol.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.BatchFetch;
import org.eclipse.persistence.annotations.BatchFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.base.util.CollectionUtils;

@Entity
@Table(name="hca_protocolclass", schema="phaedra")
@SequenceGenerator(name="hca_protocolclass_s", sequenceName="hca_protocolclass_s", schema="phaedra", allocationSize=1)
public class ProtocolClass extends PlatformObject implements IValueObject, IOwnedObject, Serializable {

	private static final long serialVersionUID = -4645008389941731706L;

	@Id
	@Column(name="protocolclass_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_protocolclass_s")
	private long id;

	@Column(name="protocolclass_name")
	private String name;
	@Column(name="description")
	private String description;

	@Column(name="is_editable")
	private boolean editable;
	@Column(name="is_in_development")
	private boolean inDevelopment;

	@IgnoreSizeOf
	@OneToMany(mappedBy="protocolClass", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<Feature> features;

	@IgnoreSizeOf
	@OneToMany(mappedBy="protocolClass", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<SubWellFeature> subWellFeatures;

	@IgnoreSizeOf
	@OneToMany(mappedBy="protocolClass", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<FeatureGroup> featureGroups;

	@IgnoreSizeOf
	@OneToMany(mappedBy="protocolClass", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<Protocol> protocols;
	
	@Column(name="low_welltype")
	private String lowWellTypeCode;

	@Column(name="high_welltype")
	private String highWellTypeCode;

	@IgnoreSizeOf
	@BatchFetch(BatchFetchType.JOIN)
	@OneToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
	@JoinColumn(name="image_setting_id")
	private ImageSettings imageSettings;

	@OneToOne
	@JoinColumn(name="default_feature_id")
	private Feature defaultFeature;
	
	@Column(name="default_lims")
	private String defaultLinkSource;
	
	@Column(name="default_layout_template")
	private String defaultTemplate;
	
	@Column(name="default_capture_config")
	private String defaultCaptureConfig;

	@Column(name="is_multi_dim_subwell_data")
	private boolean multiDimensionalSubwellData;

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

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public boolean isInDevelopment() {
		return inDevelopment;
	}

	public void setInDevelopment(boolean inDevelopment) {
		this.inDevelopment = inDevelopment;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public Feature getDefaultFeature() {
		return defaultFeature;
	}

	public void setDefaultFeature(Feature defaultFeature) {
		this.defaultFeature = defaultFeature;
	}

	public List<Feature> getFeatures() {
		return features;
	}

	public void setFeatures(List<Feature> features) {
		this.features = features;
	}

	public List<SubWellFeature> getSubWellFeatures() {
		return subWellFeatures;
	}

	public void setSubWellFeatures(List<SubWellFeature> subWellFeatures) {
		this.subWellFeatures = subWellFeatures;
	}

	public List<FeatureGroup> getFeatureGroups() {
		return featureGroups;
	}

	public void setFeatureGroups(List<FeatureGroup> featureGroups) {
		this.featureGroups = featureGroups;
	}

	public List<Protocol> getProtocols() {
		return protocols;
	}
	
	public void setProtocols(List<Protocol> protocols) {
		this.protocols = protocols;
	}
	
	public String getDefaultTemplate() {
		return defaultTemplate;
	}

	public void setDefaultTemplate(String defaultTemplate) {
		this.defaultTemplate = defaultTemplate;
	}

	public ImageSettings getImageSettings() {
		return imageSettings;
	}

	public void setImageSettings(ImageSettings imageSettings) {
		this.imageSettings = imageSettings;
	}

	public String getDefaultLinkSource() {
		return defaultLinkSource;
	}
	
	public void setDefaultLinkSource(String defaultLinkSource) {
		this.defaultLinkSource = defaultLinkSource;
	}

	public void setDefaultCaptureConfig(String defaultCaptureConfig) {
		this.defaultCaptureConfig = defaultCaptureConfig;
	}
	
	public String getDefaultCaptureConfig() {
		return defaultCaptureConfig;
	}
	
	public boolean isMultiDimensionalSubwellData() {
		return multiDimensionalSubwellData;
	}

	public void setMultiDimensionalSubwellData(boolean multiDimensionalSubwellData) {
		this.multiDimensionalSubwellData = multiDimensionalSubwellData;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

	@Override
	public String toString() {
		return name + " (" + id + ")";
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + +(int) (id ^ (id >>> 32));
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProtocolClass other = (ProtocolClass) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String[] getOwners() {
		List<String> owners = new ArrayList<>();
		if (protocols != null) {
			for (Protocol p: protocols) CollectionUtils.addUnique(owners, p.getOwners());
		}
		return owners.toArray(new String[owners.size()]);
	}

	@Override
	public IValueObject getParent() {
		return null;
	}
}
