package eu.openanalytics.phaedra.export.core.writer.format;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.datatype.description.EntityIdDescription;
import eu.openanalytics.phaedra.base.datatype.description.StringValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.TimestampDescription;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.export.core.ExportInfo;
import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;


public class AbstractExportWriter {
	
	
	private Date timestamp;
	private Protocol protocol;
	
	private IExportExperimentsSettings settings;
	private IValueConverter nameConverter;
	
	private final List<ExportInfo> additionalExportInfos = new ArrayList<>();
	
	
	public AbstractExportWriter() {
	}
	
	
	protected void initialize(final IExportExperimentsSettings settings) {
		this.settings = settings;
		
		this.timestamp = new Date();
		List<Experiment> experiments = settings.getExperiments();
		this.protocol = (!experiments.isEmpty()) ? experiments.get(0).getProtocol() : null;
	}
	
	protected IExportExperimentsSettings getSettings() {
		return this.settings;
	}
	
	protected Protocol getProtocol() {
		return this.protocol;
	}
	
	protected IValueConverter getNameConverter() {
		return this.nameConverter;
	}
	
	protected void setNameConverter(final IValueConverter converter) {
		this.nameConverter = converter;
	}
	
	
	public void addExportInfo(final ExportInfo info) {
		this.additionalExportInfos.add(info);
	}
	
	protected List<ExportInfo> getExportInfos() {
		final List<ExportInfo> infos = new ArrayList<>();
		
		if (this.protocol != null) {
			infos.add(new ExportInfo(new EntityIdDescription("Protocol ID", ExportInfo.class, Protocol.class),
					this.protocol.getId() ));
			infos.add(new ExportInfo(new StringValueDescription("Protocol Name", ExportInfo.class),
					this.protocol.getName() ));
			infos.add(new ExportInfo(new EntityIdDescription("Protocol Class ID", ExportInfo.class, ProtocolClass.class),
					this.protocol.getProtocolClass().getId() ));
			infos.add(new ExportInfo(new StringValueDescription("Protocol Class Name", ExportInfo.class),
					this.protocol.getProtocolClass().getName() ));
			infos.add(new ExportInfo(new EntityIdDescription("Experiment ID", ExportInfo.class, Experiment.class),
					this.settings.getExperiments().stream().map((experiment) -> (double)experiment.getId()).collect(Collectors.toList()) )); 
			infos.add(new ExportInfo(new StringValueDescription("Experiment Name", ExportInfo.class),
					this.settings.getExperiments().stream().map((experiment) -> experiment.getName()).collect(Collectors.toList()) ));
		}
		infos.add(new ExportInfo(new StringValueDescription("Export User", ExportInfo.class),
				SecurityService.getInstance().getCurrentUserName() ));
		infos.add(new ExportInfo(new TimestampDescription("Export Timestamp", ExportInfo.class),
				this.timestamp ));
		
		infos.addAll(this.additionalExportInfos);
		
		return infos;
	}
	
	
}
