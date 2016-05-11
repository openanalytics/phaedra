package eu.openanalytics.phaedra.model.curve.vo;

import java.io.Serializable;

import eu.openanalytics.phaedra.model.curve.CurveProperty;

public class CurveSettings implements Serializable {

	private static final long serialVersionUID = 5138850321231482385L;

	public static final String KIND = CurveProperty.KIND.toString();
	public static final String METHOD = CurveProperty.METHOD.toString();
	public static final String MODEL = CurveProperty.MODEL.toString();
	public static final String TYPE = CurveProperty.TYPE.toString();
	public static final String THRESHOLD = CurveProperty.THRESHOLD.toString();
	public static final String LB = CurveProperty.LB.toString();
	public static final String UB = CurveProperty.UB.toString();
	
	public static final String GROUP_BY_1 = CurveProperty.GROUP_BY_1.toString();
	public static final String GROUP_BY_2 = CurveProperty.GROUP_BY_2.toString();
	public static final String GROUP_BY_3 = CurveProperty.GROUP_BY_3.toString();

	private String kind;
	private String method;
	private String model;
	private String type;

	private double threshold;
	private double lb;
	private double ub;

	private String groupBy1;
	private String groupBy2;
	private String groupBy3;
	
	public String getKind() {
		return kind;
	}
	public void setKind(String kind) {
		this.kind = kind;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public double getThreshold() {
		return threshold;
	}
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	public double getLb() {
		return lb;
	}
	public void setLb(double lb) {
		this.lb = lb;
	}
	public double getUb() {
		return ub;
	}
	public void setUb(double ub) {
		this.ub = ub;
	}
	public String getGroupBy1() {
		return groupBy1;
	}
	public void setGroupBy1(String groupBy1) {
		this.groupBy1 = groupBy1;
	}
	public String getGroupBy2() {
		return groupBy2;
	}
	public void setGroupBy2(String groupBy2) {
		this.groupBy2 = groupBy2;
	}
	public String getGroupBy3() {
		return groupBy3;
	}
	public void setGroupBy3(String groupBy3) {
		this.groupBy3 = groupBy3;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		long temp;
		temp = Double.doubleToLongBits(lb);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((model == null) ? 0 : model.hashCode());
		temp = Double.doubleToLongBits(threshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		temp = Double.doubleToLongBits(ub);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		CurveSettings other = (CurveSettings) obj;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		if (Double.doubleToLongBits(lb) != Double.doubleToLongBits(other.lb))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (model == null) {
			if (other.model != null)
				return false;
		} else if (!model.equals(other.model))
			return false;
		if (Double.doubleToLongBits(threshold) != Double
				.doubleToLongBits(other.threshold))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (Double.doubleToLongBits(ub) != Double.doubleToLongBits(other.ub))
			return false;
		return true;
	}
}
