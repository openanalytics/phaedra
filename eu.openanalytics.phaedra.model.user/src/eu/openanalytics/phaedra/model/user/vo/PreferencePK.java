package eu.openanalytics.phaedra.model.user.vo;

import javax.persistence.Embeddable;

@Embeddable
public class PreferencePK {

	private String type;
	private String user;
	private String item;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
}
