package eu.openanalytics.phaedra.silo.vo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;

@Entity
@Table(name="hca_silo_dataset_column", schema="phaedra")
@SequenceGenerator(name="hca_silo_dataset_column_s", sequenceName="hca_silo_dataset_column_s", schema="phaedra", allocationSize=1)
public class SiloDatasetColumn extends PlatformObject implements IValueObject, Serializable {

	private static final long serialVersionUID = -6137758484301997499L;

	@Id
	@Column(name="column_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_silo_dataset_column_s")
	private long id;

	@Column(name="column_name")
	private String name;
	
	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="dataset_id")
	private SiloDataset dataset;
	
	@Column(name="data_type")
	@Enumerated(EnumType.STRING)
	private SiloDataType type;

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

	public SiloDataset getDataset() {
		return dataset;
	}

	public void setDataset(SiloDataset dataset) {
		this.dataset = dataset;
	}

	public SiloDataType getType() {
		return type;
	}

	public void setType(SiloDataType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return name + "(" + id + ")";
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
		SiloDatasetColumn other = (SiloDatasetColumn) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return dataset;
	}

}
