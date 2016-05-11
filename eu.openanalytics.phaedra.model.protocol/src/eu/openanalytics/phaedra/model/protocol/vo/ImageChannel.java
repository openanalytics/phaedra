package eu.openanalytics.phaedra.model.protocol.vo;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;

@Entity
@Table(name="hca_image_channel", schema="phaedra")
@SequenceGenerator(name="hca_image_channel_s", sequenceName="hca_image_channel_s", schema="phaedra", allocationSize=1)
public class ImageChannel implements IValueObject, Serializable {

	private static final long serialVersionUID = -3393024177197904976L;
	public static final int CHANNEL_TYPE_RAW = 0;
	public static final int CHANNEL_TYPE_OVERLAY = 1;
	public static final int CHANNEL_TYPE_MANUAL_LOOKUP = 2;
	public static final int CHANNEL_TYPE_LABEL = 3;
	public static final int CHANNEL_TYPE_LOOKUP = 4;

	public static final int CHANNEL_SOURCE_JP2K = 1;
	public static final int CHANNEL_SOURCE_HDF5 = 2;

	public static final int CHANNEL_BIT_DEPTH_1 = 1;
	public static final int CHANNEL_BIT_DEPTH_8 = 8;
	public static final int CHANNEL_BIT_DEPTH_16 = 16;

	@Id
	@Column(name="image_channel_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_image_channel_s")
	private long id;

	@Column(name="channel_name")
	private String name;
	@Column(name="description")
	private String description;
	@Column(name="channel_type")
	private int type;
	@Column(name="channel_sequence")
	private int sequence;
	@Column(name="channel_source")
	private int source;

	@Column(name="color_mask")
	private int colorMask;
	@Column(name="lookup_low")
	private int lookupLow;
	@Column(name="lookup_high")
	private int lookupHigh;

	@Column(name="show_in_plate")
	private boolean showInPlateView;
	@Column(name="show_in_well")
	private boolean showInWellView;

	@Column(name="alpha")
	private int alpha;
	@Column(name="level_min")
	private int levelMin;
	@Column(name="level_max")
	private int levelMax;

	@Column(name="bit_depth")
	private int bitDepth;

	@IgnoreSizeOf
	@ElementCollection
	@MapKeyColumn(name="setting_name")
	@Column(name="setting_value")
	@CollectionTable(name="hca_image_channel_config", schema="phaedra", joinColumns=@JoinColumn(name="image_channel_id"))
	private Map<String,String> channelConfig;

	@ManyToOne
    @JoinColumn(name="image_setting_id")
	private ImageSettings imageSettings;

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
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getSequence() {
		return sequence;
	}
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
	public int getSource() {
		return source;
	}
	public void setSource(int source) {
		this.source = source;
	}
	public int getColorMask() {
		return colorMask;
	}
	public void setColorMask(int colorMask) {
		this.colorMask = colorMask;
	}
	public int getLookupLow() {
		return lookupLow;
	}
	public void setLookupLow(int lookupLow) {
		this.lookupLow = lookupLow;
	}
	public int getLookupHigh() {
		return lookupHigh;
	}
	public void setLookupHigh(int lookupHigh) {
		this.lookupHigh = lookupHigh;
	}
	public boolean isShowInPlateView() {
		return showInPlateView;
	}
	public void setShowInPlateView(boolean showInPlateView) {
		this.showInPlateView = showInPlateView;
	}
	public boolean isShowInWellView() {
		return showInWellView;
	}
	public void setShowInWellView(boolean showInWellView) {
		this.showInWellView = showInWellView;
	}
	public int getAlpha() {
		return alpha;
	}
	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}
	public int getLevelMin() {
		return levelMin;
	}
	public void setLevelMin(int levelMin) {
		this.levelMin = levelMin;
	}
	public int getLevelMax() {
		return levelMax;
	}
	public void setLevelMax(int levelMax) {
		this.levelMax = levelMax;
	}
	public int getBitDepth() {
		return bitDepth;
	}
	public void setBitDepth(int bitDepth) {
		this.bitDepth = bitDepth;
	}
	public Map<String, String> getChannelConfig() {
		return channelConfig;
	}
	public void setChannelConfig(Map<String, String> channelConfig) {
		this.channelConfig = channelConfig;
	}
	public ImageSettings getImageSettings() {
		return imageSettings;
	}
	public void setImageSettings(ImageSettings imageSettings) {
		this.imageSettings = imageSettings;
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
		ImageChannel other = (ImageChannel) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return getImageSettings();
	}
}
