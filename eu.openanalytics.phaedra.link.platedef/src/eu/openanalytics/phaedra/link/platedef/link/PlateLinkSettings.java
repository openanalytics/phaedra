package eu.openanalytics.phaedra.link.platedef.link;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.model.plate.vo.Plate;


public class PlateLinkSettings {

	private Plate plate;
	private String barcode;
	private Map<String, Object> settings;
	
	public PlateLinkSettings() {
		settings = new HashMap<String, Object>();
	}
	
	public Plate getPlate() {
		return plate;
	}
	public void setPlate(Plate plate) {
		this.plate = plate;
	}
	public String getBarcode() {
		return barcode;
	}
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}
	public Map<String, Object> getSettings() {
		return settings;
	}
	public void setSettings(Map<String, Object> settings) {
		this.settings = settings;
	}
}
