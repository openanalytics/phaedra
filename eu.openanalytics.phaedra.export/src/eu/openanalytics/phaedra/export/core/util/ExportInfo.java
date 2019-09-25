package eu.openanalytics.phaedra.export.core.util;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class ExportInfo {

	public static Info[] get(List<Experiment> experiments, Date timestamp) {
		if (experiments == null || experiments.isEmpty()) return new Info[0];
		Protocol protocol = experiments.get(0).getProtocol();
		
		Info[] infos = new Info[] {
				new Info("Protocol ID", QueryResult.DOUBLE_VALUE, (double)protocol.getId()),
				new Info("Protocol Name", QueryResult.STRING_VALUE, protocol.getName()),
				new Info("Protocol Class ID", QueryResult.DOUBLE_VALUE, (double)protocol.getProtocolClass().getId()),
				new Info("Protocol Class Name", QueryResult.STRING_VALUE, protocol.getProtocolClass().getName()),
				new Info("Experiment ID", QueryResult.DOUBLE_VALUE,
						experiments.stream().map((experiment) -> (double)experiment.getId()).collect(Collectors.toList())), 
				new Info("Experiment Name", QueryResult.STRING_VALUE,
						experiments.stream().map((experiment) -> experiment.getName()).collect(Collectors.toList())), 
				new Info("Export User", QueryResult.STRING_VALUE, SecurityService.getInstance().getCurrentUserName()),
				new Info("Export Timestamp", QueryResult.TIMESTAMP_VALUE, timestamp) };
		return infos;
	}
	
	public static class Info {
		
		public String name;
		public byte valueType;
		public List<?> values;
		
		public Info(String name, byte valueType, List<?> values) {
			this.name= name;
			this.valueType = valueType;
			this.values = values;
		}
		
		public Info(String name, byte valueType, Object value) {
			this.name= name;
			this.valueType = valueType;
			this.values = Collections.singletonList(value);
		}
		
	}
}
