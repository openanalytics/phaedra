package eu.openanalytics.phaedra.ui.partsettings.vo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.db.jpa.converter.PropertiesConverter;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

@IgnoreSizeOf
@Entity
@Table(name="hca_part_settings", schema="phaedra")
@Converter(name="propertiesConverter", converterClass=PropertiesConverter.class)
@SequenceGenerator(name="hca_part_settings_s", sequenceName="hca_part_settings_s", schema="phaedra", allocationSize=1)
public class PartSettings extends PlatformObject implements IValueObject, Serializable {

	private static final long serialVersionUID = 764113069395604585L;

	@Id
	@Column(name="settings_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_part_settings_s")
	private long id;

	@JoinColumn(name="protocol_id")
	private Protocol protocol;

	@Column(name="user_code")
	private String userName;

	@Column(name="class_name")
	private String className;

	@Convert("propertiesConverter")
	@Column(name="properties")
	private Properties properties;

	@Column(name="name")
	private String name;

	@Column(name="is_global")
	private boolean isGlobal;

	@Column(name="is_template")
	private boolean isTemplate;

	@Override
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public boolean isGlobal() {
		return isGlobal;
	}

	public void setGlobal(boolean isGlobal) {
		this.isGlobal = isGlobal;
	}

	public boolean isTemplate() {
		return isTemplate;
	}

	public void setTemplate(boolean isTemplate) {
		this.isTemplate = isTemplate;
	}

	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */

	/**
	 * <p>Returns the settings name with an indication if the settings are global and/or a template.
	 * </p><p>
	 * Global settings will be indicated with <code>[G]</code>, template settings with <code>[T]</code> and global template settings with <code>[GT]</code>.
	 * </p>
	 * @return
	 */
	public String getDisplayName() {
		StringBuilder builder = new StringBuilder();
		builder.append(getName());

		boolean isGlobalOrTemplate = isGlobal() || isTemplate();
		if (isGlobalOrTemplate) builder.append(" [");
		if (isGlobal()) builder.append("G");
		if (isTemplate()) builder.append("T");
		if (isGlobalOrTemplate) builder.append("]");

		return builder.toString();
	}

	@Override
	public String toString() {
		return "Part Settings " + name  + " (" + id + ")";
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
		PartSettings other = (PartSettings) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return protocol;
	}

}
