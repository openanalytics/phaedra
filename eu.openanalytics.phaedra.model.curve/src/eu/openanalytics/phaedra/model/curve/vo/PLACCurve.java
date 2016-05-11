package eu.openanalytics.phaedra.model.curve.vo;

public class PLACCurve extends Curve {

	private static final long serialVersionUID = 7431572500925031438L;

	private double plac;
	private String placCensor;
	private double threshold;

	private double lcMean;
	private double lcStdev;
	private double hcMean;
	private double hcStdev;

	private int nic;
	private int nac;
	
	public double getPlac() {
		return plac;
	}
	public void setPlac(double plac) {
		this.plac = plac;
	}
	public String getPlacCensor() {
		return placCensor;
	}
	public void setPlacCensor(String placCensor) {
		this.placCensor = placCensor;
	}
	public double getThreshold() {
		return threshold;
	}
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public double getLcMean() {
		return lcMean;
	}
	public void setLcMean(double lcMean) {
		this.lcMean = lcMean;
	}
	public double getLcStdev() {
		return lcStdev;
	}
	public void setLcStdev(double lcStdev) {
		this.lcStdev = lcStdev;
	}
	public double getHcMean() {
		return hcMean;
	}
	public void setHcMean(double hcMean) {
		this.hcMean = hcMean;
	}
	public double getHcStdev() {
		return hcStdev;
	}
	public void setHcStdev(double hcStdev) {
		this.hcStdev = hcStdev;
	}
	public int getNic() {
		return nic;
	}
	public void setNic(int nic) {
		this.nic = nic;
	}
	public int getNac() {
		return nac;
	}
	public void setNac(int nac) {
		this.nac = nac;
	}
}
