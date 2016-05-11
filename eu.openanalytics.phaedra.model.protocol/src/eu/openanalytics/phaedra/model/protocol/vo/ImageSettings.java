package eu.openanalytics.phaedra.model.protocol.vo;

import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;

@Entity
@Table(name="hca_image_setting", schema="phaedra")
@SequenceGenerator(name="hca_image_setting_s", sequenceName="hca_image_setting_s", schema="phaedra", allocationSize=1)
public class ImageSettings implements IValueObject, Serializable {

	private static final long serialVersionUID = 3135118530893548566L;

	@Id
	@Column(name="image_setting_id")
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="hca_image_setting_s")
	private long id;

	@Column(name="zoom_ratio")
	private int zoomRatio;
	@Column(name="gamma")
	private int gamma;

	@Column(name="pixel_size_x")
	private float pixelSizeX;

	@Column(name="pixel_size_y")
	private float pixelSizeY;

	@Column(name="pixel_size_z")
	private float pixelSizeZ;

	@IgnoreSizeOf
	@OneToMany(mappedBy="imageSettings", fetch=FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval=true)
	@OrderColumn(name="channel_sequence")
	private List<ImageChannel> imageChannels;

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
	public int getZoomRatio() {
		return zoomRatio;
	}
	public void setZoomRatio(int zoomRatio) {
		this.zoomRatio = zoomRatio;
	}
	public int getGamma() {
		return gamma;
	}
	public void setGamma(int gamma) {
		this.gamma = gamma;
	}
	public List<ImageChannel> getImageChannels() {
		return imageChannels;
	}
	public void setImageChannels(List<ImageChannel> imageChannels) {
		this.imageChannels = imageChannels;
	}
	public float getPixelSizeX() {
		return pixelSizeX;
	}
	public void setPixelSizeX(float pixelSizeX) {
		this.pixelSizeX = pixelSizeX;
	}
	public float getPixelSizeY() {
		return pixelSizeY;
	}
	public void setPixelSizeY(float pixelSizeY) {
		this.pixelSizeY = pixelSizeY;
	}
	public float getPixelSizeZ() {
		return pixelSizeZ;
	}
	public void setPixelSizeZ(float pixelSizeZ) {
		this.pixelSizeZ = pixelSizeZ;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (" + id + ")";
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
		ImageSettings other = (ImageSettings) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		// Could be a protocol or protocol class, cannot be determined here.
		return null;
	}
}
