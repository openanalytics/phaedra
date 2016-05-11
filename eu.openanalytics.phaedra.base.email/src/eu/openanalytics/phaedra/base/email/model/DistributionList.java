package eu.openanalytics.phaedra.base.email.model;

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name="hca_mail_distribution_list", schema="phaedra")
@SequenceGenerator(name="hca_mail_distribution_list_s", sequenceName="hca_mail_distribution_list_s", schema="phaedra", allocationSize=1)
public class DistributionList {

	@Id
	@Column(name="list_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="hca_mail_distribution_list_s")
	private long id;
	
	@Column(name="list_name")
	private String name;
	
	@Column(name="label")
	private String label;
	
	@ElementCollection
	@CollectionTable(name="hca_mail_list_member", schema="phaedra", joinColumns=@JoinColumn(name="list_id"))
	@Column(name="email_address")
	private List<String> subscribers;

	/*
	 * *****************
	 * Getters & setters
	 * *****************
	 */
	
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

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<String> getSubscribers() {
		return subscribers;
	}

	public void setSubscribers(List<String> subscribers) {
		this.subscribers = subscribers;
	}
	
	/*
	 * *******************
	 * Convenience methods
	 * *******************
	 */
	
	@Override
	public String toString() {
		return name + " (" + label + ") (" + subscribers.size() + " subscribers)";
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
		DistributionList other = (DistributionList) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
}
