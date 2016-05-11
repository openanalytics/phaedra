package eu.openanalytics.phaedra.model.curve;

import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

import eu.openanalytics.phaedra.model.curve.CurveService.CurveKind;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.OSBCurve;
import eu.openanalytics.phaedra.model.curve.vo.PLACCurve;

public enum CurveProperty {

	ID(null, "Id", c-> c.getId(), (c,v) -> c.setId((Long) v)),
	
	// General settings (independent of curve kind)
	
	KIND(null, "Kind", c-> c.getSettings().getKind(), (c,v) -> c.getSettings().setKind((String) v)),
	METHOD(null, "Method", c-> c.getSettings().getMethod(), (c,v) -> c.getSettings().setMethod((String) v)),
	MODEL(null, "Model", c-> c.getSettings().getModel(), (c,v) -> c.getSettings().setModel((String) v)),
	TYPE(null, "Type", c-> c.getSettings().getType(), (c,v) -> c.getSettings().setType((String) v)),
	
	GROUP_BY_1(null, "Group By 1", c-> c.getSettings().getGroupBy1(), (c,v) -> c.getSettings().setGroupBy1((String) v)),
	GROUP_BY_2(null, "Group By 2", c-> c.getSettings().getGroupBy2(), (c,v) -> c.getSettings().setGroupBy2((String) v)),
	GROUP_BY_3(null, "Group By 3", c-> c.getSettings().getGroupBy3(), (c,v) -> c.getSettings().setGroupBy3((String) v)),
	
	// Curve-specific settings
	
	LB(CurveKind.OSB, "Manual LB", c-> c.getSettings().getLb(), (c,v) -> c.getSettings().setLb((Double) v)),
	UB(CurveKind.OSB, "Manual UB", c-> c.getSettings().getUb(), (c,v) -> c.getSettings().setUb((Double) v)),
	THRESHOLD(CurveKind.PLAC, "Manual Threshold", c-> c.getSettings().getThreshold(), (c,v) -> c.getSettings().setThreshold((Double) v)),
	
	// General properties (independent of curve kind)
	
	FIT_DATE(null, "Fit Date", c-> c.getFitDate(), (c,v) -> c.setFitDate((Date) v)),
	FIT_ERROR(null, "Fit Error", c-> c.getFitError(), (c,v) -> c.setFitError((Integer) v)),
	FIT_MESSAGE(null, "Fit Message", c-> c.getFitMessage(), (c,v) -> c.setFitMessage((String) v)),
	FIT_VERSION(null, "Fit Version", c-> c.getFitVersion(), (c,v) -> c.setFitVersion((String) v)),
	EMAX(null, "eMax", c-> c.geteMax(), (c,v) -> c.seteMax((Double) v)),
	EMAX_CONC(null, "eMax Conc", c-> c.geteMaxConc(), (c,v) -> c.seteMaxConc((Double) v)),
	PLOT(null, "Plot", c-> c.getPlot(), (c,v) -> c.setPlot((byte[]) v)),
	
	// OSB properties
	
	HILL(CurveKind.OSB, "Hill", c-> ((OSBCurve)c).getHill(), (c,v) -> ((OSBCurve)c).setHill((Double) v)),
	HILL_STDERR(CurveKind.OSB, "Hill StdErr", c-> ((OSBCurve)c).getHillStdErr(), (c,v) -> ((OSBCurve)c).setHillStdErr((Double) v)),
	PIC50(CurveKind.OSB, "pIC50", c-> ((OSBCurve)c).getPic50(), (c,v) -> ((OSBCurve)c).setPic50((Double) v)),
	PIC50_CENSOR(CurveKind.OSB, "pIC50 Censor", c-> ((OSBCurve)c).getPic50Censor(), (c,v) -> ((OSBCurve)c).setPic50Censor((String) v)),
	PIC50_STDERR(CurveKind.OSB, "pIC50 StdErr", c-> ((OSBCurve)c).getPic50StdErr(), (c,v) -> ((OSBCurve)c).setPic50StdErr((Double) v)),
	PIC50_LB(CurveKind.OSB, "pIC50 LB", c-> ((OSBCurve)c).getLb(), (c,v) -> ((OSBCurve)c).setLb((Double) v)),
	PIC50_LB_STDERR(CurveKind.OSB, "pIC50 LB StdErr", c-> ((OSBCurve)c).getLbStdErr(), (c,v) -> ((OSBCurve)c).setLbStdErr((Double) v)),
	PIC50_UB(CurveKind.OSB, "pIC50 UB", c-> ((OSBCurve)c).getUb(), (c,v) -> ((OSBCurve)c).setUb((Double) v)),
	PIC50_UB_STDERR(CurveKind.OSB, "pIC50 UB StdErr", c-> ((OSBCurve)c).getUbStdErr(), (c,v) -> ((OSBCurve)c).setUbStdErr((Double) v)),
	PIC50_LCL(CurveKind.OSB, "pIC50 LCL", c-> ((OSBCurve)c).getPic50Lcl(), (c,v) -> ((OSBCurve)c).setPic50Lcl((Double) v)),
	PIC50_UCL(CurveKind.OSB, "pIC50 UCL", c-> ((OSBCurve)c).getPic50Ucl(), (c,v) -> ((OSBCurve)c).setPic50Ucl((Double) v)),
	PIC20(CurveKind.OSB, "pIC20", c-> ((OSBCurve)c).getPic20(), (c,v) -> ((OSBCurve)c).setPic20((Double) v)),
	PIC80(CurveKind.OSB, "pIC80", c-> ((OSBCurve)c).getPic80(), (c,v) -> ((OSBCurve)c).setPic80((Double) v)),
	PLATE_LB(CurveKind.OSB, "Plate LB", c-> ((OSBCurve)c).getPlateLb(), (c,v) -> ((OSBCurve)c).setPlateLb((Double) v)),
	PLATE_UB(CurveKind.OSB, "Plate UB", c-> ((OSBCurve)c).getPlateUb(), (c,v) -> ((OSBCurve)c).setPlateUb((Double) v)),
	CI_GRID(CurveKind.OSB, "Confidence Band", c-> ((OSBCurve)c).getCiGrid(), (c,v) -> ((OSBCurve)c).setCiGrid((double[][]) v)),
	WEIGHTS(CurveKind.OSB, "Weights", c-> ((OSBCurve)c).getWeights(), (c,v) -> ((OSBCurve)c).setWeights((double[]) v)),
	R2(CurveKind.OSB, "r2", c-> ((OSBCurve)c).getR2(), (c,v) -> ((OSBCurve)c).setR2((Double) v)),
	AIC(CurveKind.OSB, "AIC", c-> ((OSBCurve)c).getAic(), (c,v) -> ((OSBCurve)c).setAic((Double) v)),
	BIC(CurveKind.OSB, "BIC", c-> ((OSBCurve)c).getBic(), (c,v) -> ((OSBCurve)c).setBic((Double) v)),
	DFE(CurveKind.OSB, "DFE", c-> ((OSBCurve)c).getDfe(), (c,v) -> ((OSBCurve)c).setDfe((Double) v)),
	SE(CurveKind.OSB, "SE", c-> ((OSBCurve)c).getSe(), (c,v) -> ((OSBCurve)c).setSe((Double) v)),
	
	// pLAC properties
	
	PLAC(CurveKind.PLAC, "pLAC", c-> ((PLACCurve)c).getPlac(), (c,v) -> ((PLACCurve)c).setPlac((Double) v)),
	PLAC_CENSOR(CurveKind.PLAC, "pLAC Censor", c-> ((PLACCurve)c).getPlacCensor(), (c,v) -> ((PLACCurve)c).setPlacCensor((String) v)),
	PLAC_THRESHOLD(CurveKind.PLAC, "pLAC Threshold", c-> ((PLACCurve)c).getThreshold(), (c,v) -> ((PLACCurve)c).setThreshold((Double) v)),
	HC_MEAN(CurveKind.PLAC, "HC Mean", c-> ((PLACCurve)c).getHcMean(), (c,v) -> ((PLACCurve)c).setHcMean((Double) v)),
	HC_STDEV(CurveKind.PLAC, "HC StDev", c-> ((PLACCurve)c).getHcStdev(), (c,v) -> ((PLACCurve)c).setHcStdev((Double) v)),
	LC_MEAN(CurveKind.PLAC, "LC Mean", c-> ((PLACCurve)c).getLcMean(), (c,v) -> ((PLACCurve)c).setLcMean((Double) v)),
	LC_STDEV(CurveKind.PLAC, "LC StDev", c-> ((PLACCurve)c).getLcStdev(), (c,v) -> ((PLACCurve)c).setLcStdev((Double) v)),
	NIC(CurveKind.PLAC, "Nic", c-> ((PLACCurve)c).getNic(), (c,v) -> ((PLACCurve)c).setNic(((Number) v).intValue())),
	NAC(CurveKind.PLAC, "Nac", c-> ((PLACCurve)c).getNac(), (c,v) -> ((PLACCurve)c).setNac(((Number) v).intValue()))
	;

	private CurveKind kind;
	private String label;
	private Function<Curve, Object> valueGetter;
	private BiConsumer<Curve, Object> valueSetter;

	private CurveProperty(CurveKind kind, String label, Function<Curve, Object> valueGetter, BiConsumer<Curve, Object> valueSetter) {
		this.kind = kind;
		this.label = label;
		this.valueGetter = valueGetter;
		this.valueSetter = valueSetter;
	}

	public String getLabel() {
		return label;
	}
	
	public Object getValue(Curve curve) {
		if (valueGetter == null || curve == null) return null;
		return valueGetter.apply(curve);
	}
	
	public void setValue(Curve curve, Object value) {
		if (valueSetter == null) throw new UnsupportedOperationException("Cannot set value: no value setter defined for " + toString());
		valueSetter.accept(curve, value);
	}
	
	public boolean sameKind(Curve curve) {
		return curve != null && kind != null && kind.toString().equals(curve.getSettings().getKind());
	}
	
	public boolean appliesTo(Curve curve) {
		return kind == null || sameKind(curve);
	}
		
	public static CurveProperty getByLabel(String label) {
		for (CurveProperty p: values()) {
			if (label.equals(p.getLabel())) return p;
		}
		return null;
	}
	
	public static CurveProperty getByName(String name) {
		for (CurveProperty p: values()) {
			if (name.equals(p.name())) return p;
		}
		return null;
	}
}