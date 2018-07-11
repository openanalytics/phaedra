package eu.openanalytics.phaedra.silo.vo;

import java.io.Serializable;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.persistence.annotations.JoinFetch;
import org.eclipse.persistence.annotations.JoinFetchType;

import eu.openanalytics.phaedra.base.cache.IgnoreSizeOf;
import eu.openanalytics.phaedra.base.db.IValueObject;

@Entity
@Table(name="hca_silo_dataset", schema="phaedra")
@SequenceGenerator(name="hca_silo_dataset_s", sequenceName="hca_silo_dataset_s", schema="phaedra", allocationSize=1)
public class SiloDataset extends PlatformObject implements IValueObject, Serializable {
	
	private static final long serialVersionUID = 6047482571534456587L;

	@Id
	@Column(name="dataset_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_silo_dataset_s")
	private long id;

	@Column(name="dataset_name")
	private String name;
	
	@IgnoreSizeOf
	@JoinFetch(JoinFetchType.INNER)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="silo_id")
	private Silo silo;

	@IgnoreSizeOf
	@OneToMany(mappedBy="dataset", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<SiloDatasetColumn> columns;
	
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

	public Silo getSilo() {
		return silo;
	}

	public void setSilo(Silo silo) {
		this.silo = silo;
	}
	
	public List<SiloDatasetColumn> getColumns() {
		return columns;
	}
	
	public void setColumns(List<SiloDatasetColumn> columns) {
		this.columns = columns;
	}

	@Override
	public String toString() {
		return name + "(" + id + ") (silo: " + silo.getId() + ")";
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
		SiloDataset other = (SiloDataset) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public IValueObject getParent() {
		return silo;
	}
	
}
