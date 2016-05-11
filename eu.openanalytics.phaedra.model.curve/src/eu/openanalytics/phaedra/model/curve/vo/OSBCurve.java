package eu.openanalytics.phaedra.model.curve.vo;

public class OSBCurve extends Curve {

	private static final long serialVersionUID = 181223798193557751L;

	private double pic50;
	private String pic50Censor;
	private double pic50StdErr;
	private double pic50Lcl;
	private double pic50Ucl;
	private double lb;
	private double lbStdErr;
	private double ub;
	private double ubStdErr;
	private double se;
	private double dfe;
	private double aic;
	private double bic;
	private double r2;
	private double hill;
	private double hillStdErr;

	private double pic20;
	private double pic80;

	private double plateLb;
	private double plateUb;

	private double[][] ciGrid;
	private double[] weights;
	
	public double getPic50() {
		return pic50;
	}
	public void setPic50(double pic50) {
		this.pic50 = pic50;
	}
	public String getPic50Censor() {
		return pic50Censor;
	}
	public void setPic50Censor(String pic50Censor) {
		this.pic50Censor = pic50Censor;
	}
	public double getPic50StdErr() {
		return pic50StdErr;
	}
	public void setPic50StdErr(double pic50StdErr) {
		this.pic50StdErr = pic50StdErr;
	}
	public double getPic50Lcl() {
		return pic50Lcl;
	}
	public void setPic50Lcl(double pic50Lcl) {
		this.pic50Lcl = pic50Lcl;
	}
	public double getPic50Ucl() {
		return pic50Ucl;
	}
	public void setPic50Ucl(double pic50Ucl) {
		this.pic50Ucl = pic50Ucl;
	}
	public double getLb() {
		return lb;
	}
	public void setLb(double lb) {
		this.lb = lb;
	}
	public double getLbStdErr() {
		return lbStdErr;
	}
	public void setLbStdErr(double lbStdErr) {
		this.lbStdErr = lbStdErr;
	}
	public double getUb() {
		return ub;
	}
	public void setUb(double ub) {
		this.ub = ub;
	}
	public double getUbStdErr() {
		return ubStdErr;
	}
	public void setUbStdErr(double ubStdErr) {
		this.ubStdErr = ubStdErr;
	}
	public double getSe() {
		return se;
	}
	public void setSe(double se) {
		this.se = se;
	}
	public double getDfe() {
		return dfe;
	}
	public void setDfe(double dfe) {
		this.dfe = dfe;
	}
	public double getAic() {
		return aic;
	}
	public void setAic(double aic) {
		this.aic = aic;
	}
	public double getBic() {
		return bic;
	}
	public void setBic(double bic) {
		this.bic = bic;
	}
	public double getR2() {
		return r2;
	}
	public void setR2(double r2) {
		this.r2 = r2;
	}
	public double getHill() {
		return hill;
	}
	public void setHill(double hill) {
		this.hill = hill;
	}
	public double getHillStdErr() {
		return hillStdErr;
	}
	public void setHillStdErr(double hillStdErr) {
		this.hillStdErr = hillStdErr;
	}
	public double getPic20() {
		return pic20;
	}
	public void setPic20(double pic20) {
		this.pic20 = pic20;
	}
	public double getPic80() {
		return pic80;
	}
	public void setPic80(double pic80) {
		this.pic80 = pic80;
	}
	public double getPlateLb() {
		return plateLb;
	}
	public void setPlateLb(double plateLb) {
		this.plateLb = plateLb;
	}
	public double getPlateUb() {
		return plateUb;
	}
	public void setPlateUb(double plateUb) {
		this.plateUb = plateUb;
	}
	public double[][] getCiGrid() {
		return ciGrid;
	}
	public void setCiGrid(double[][] ciGrid) {
		this.ciGrid = ciGrid;
	}
	public double[] getWeights() {
		return weights;
	}
	public void setWeights(double[] weights) {
		this.weights = weights;
	}
}
