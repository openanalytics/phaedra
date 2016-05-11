package eu.openanalytics.phaedra.model.curve.util;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.curve.CurveProperty;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CurveSettingsMapper {

	public static CurveSettings toSettings(Feature feature) {
		
		CurveSettings settings = new CurveSettings();
		
		String kindString = feature.getCurveSettings().get(CurveSettings.KIND);
		settings.setKind(kindString);
		
		String method = feature.getCurveSettings().get(CurveSettings.METHOD);
		settings.setMethod(method);
		
		String model = feature.getCurveSettings().get(CurveSettings.MODEL);
		settings.setModel(model);
		
		String type = feature.getCurveSettings().get(CurveSettings.TYPE);
		settings.setType(type);
		
		String thString = feature.getCurveSettings().get(CurveSettings.THRESHOLD);
		double th = Double.NaN;
		if (thString != null && !thString.isEmpty() && NumberUtils.isDouble(thString)) th = Double.parseDouble(thString);
		if (Double.isNaN(th)) th = CurveFactory.getDefaultThreshold(feature);
		settings.setThreshold(th);
		
		String lbString = feature.getCurveSettings().get(CurveSettings.LB);
		double lb = Double.NaN;
		if (lbString != null && !lbString.isEmpty() && NumberUtils.isDouble(lbString)) lb = Double.parseDouble(lbString);
		settings.setLb(lb);
		
		String ubString = feature.getCurveSettings().get(CurveSettings.UB);
		double ub = Double.NaN;
		if (ubString != null && !ubString.isEmpty() && NumberUtils.isDouble(ubString)) ub = Double.parseDouble(ubString);
		settings.setUb(ub);
		
		String groupBy1 = feature.getCurveSettings().get(CurveSettings.GROUP_BY_1);
		settings.setGroupBy1(groupBy1);
		
		String groupBy2 = feature.getCurveSettings().get(CurveSettings.GROUP_BY_2);
		settings.setGroupBy2(groupBy2);
		
		String groupBy3 = feature.getCurveSettings().get(CurveSettings.GROUP_BY_3);
		settings.setGroupBy3(groupBy3);
		
		return settings;
	}
	
	public static Map<String,String> toMap(CurveSettings settings) {
		Map<String,String> map = new HashMap<>();
		
		map.put(CurveSettings.KIND, settings.getKind());
		map.put(CurveSettings.METHOD, settings.getMethod());
		map.put(CurveSettings.MODEL, settings.getModel());
		map.put(CurveSettings.TYPE, settings.getType());
		map.put(CurveSettings.THRESHOLD, "" + settings.getThreshold());
		map.put(CurveSettings.LB, "" + settings.getLb());
		map.put(CurveSettings.UB, "" + settings.getUb());
		if (settings.getGroupBy1() != null) map.put(CurveSettings.GROUP_BY_1, settings.getGroupBy1());
		if (settings.getGroupBy2() != null) map.put(CurveSettings.GROUP_BY_2, settings.getGroupBy2());
		if (settings.getGroupBy3() != null) map.put(CurveSettings.GROUP_BY_3, settings.getGroupBy3());
		return map;
	}
	
	public static void addSetting(String name, String value, CurveSettings settings) {
		if (name.equals(CurveProperty.KIND.toString())) {
			settings.setKind(value);
		} else if (name.equals(CurveProperty.METHOD.toString())) {
			settings.setMethod(value);
		} else if (name.equals(CurveProperty.MODEL.toString())) {
			settings.setModel(value);
		} else if (name.equals(CurveProperty.TYPE.toString())) {
			settings.setType(value);
		} else if (name.equals(CurveProperty.THRESHOLD.toString())) {
			double th = Double.NaN;
			if (value != null && !value.isEmpty() && NumberUtils.isDouble(value)) th = Double.parseDouble(value);
			settings.setThreshold(th);
		} else if (name.equals(CurveProperty.LB.toString())) {
			double lb = Double.NaN;
			if (value != null && !value.isEmpty() && NumberUtils.isDouble(value)) lb = Double.parseDouble(value);
			settings.setLb(lb);
		} else if (name.equals(CurveProperty.UB.toString())) {
			double ub = Double.NaN;
			if (value != null && !value.isEmpty() && NumberUtils.isDouble(value)) ub = Double.parseDouble(value);
			settings.setUb(ub);
		} else if (name.equals(CurveProperty.GROUP_BY_1.toString())) {
			settings.setGroupBy1(value);
		} else if (name.equals(CurveProperty.GROUP_BY_2.toString())) {
			settings.setGroupBy2(value);
		} else if (name.equals(CurveProperty.GROUP_BY_3.toString())) {
			settings.setGroupBy3(value);
		}
	}
}
