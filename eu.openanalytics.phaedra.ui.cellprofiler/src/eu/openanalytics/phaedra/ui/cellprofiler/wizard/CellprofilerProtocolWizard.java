package eu.openanalytics.phaedra.ui.cellprofiler.wizard;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.link.importer.ImportService;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateService;
import eu.openanalytics.phaedra.ui.cellprofiler.widget.PatternConfig;
import eu.openanalytics.phaedra.ui.cellprofiler.widget.PatternConfig.GroupRole;

public class CellprofilerProtocolWizard extends BaseStatefulWizard {

	public CellprofilerProtocolWizard() {
		setWindowTitle("Cellprofiler Protocol Wizard");
		setNeedsProgressMonitor(true);
		setPreconfiguredState(new WizardState());
	}

	@Override
	public void addPages() {
		addPage(new SelectFolderPage());
		addPage(new SelectWellDataPage());
		addPage(new SelectSubWellDataPage());
		addPage(new SelectImageDataPage());
	}

	@Override
	public boolean performFinish() {
		super.performFinish();
		Protocol p = new ProtocolCreator().run((WizardState) getState());
		if (p == null) return false;
		if (((WizardState) getState()).autoImport) return triggerImport(p);
		return true;
	}

	private boolean triggerImport(Protocol p) {
		WizardState state = (WizardState) getState();
		ImportTask task = new ImportTask();
		task.sourcePath = state.selectedFolder.toFile().getAbsolutePath();
		task.userName = SecurityService.getInstance().getCurrentUserName();

		// If user requested a new experiment, create it now.
		if (task.targetExperiment == null) {
			Experiment experiment = PlateService.getInstance().createExperiment(p);
			experiment.setName(state.selectedFolder.getFileName().toString());
			try {
				PlateService.getInstance().updateExperiment(experiment);
			} catch (Throwable t) {
				MessageDialog.openError(getShell(), "Error", "Failed to create experiment:\n" + t.getMessage());
				return false;
			}
			task.targetExperiment = experiment;
		}

		ImportService.getInstance().startJob(task);
		return true;
	}

	public static class WizardState implements IWizardState {
		public Path selectedFolder;
		public String protocolName;
		public String protocolTeam;
		public boolean autoImport;

		public Path[] dataFileCandidates;
		
		public Path selectedWellDataFile;
		public String[] wellDataHeaders;
		public String[] selectedWellDataHeaders;

		public boolean includeSubWellData;
		public boolean singleSubWellDataFile;
		
		public Path selectedSubWellDataFile;
		public String[] subWellDataHeaders;
		public String[] selectedSubWellDataHeaders;
		
		public Path subWellDataFolder;
		public String subWellDataFilePattern;
		
		public Path[] imageFolderCandidates;
		public Path selectedImageFolder;

		public List<ImageChannel> imageChannels = new ArrayList<>();
	}

	private static class ProtocolCreator {

		public Protocol run(WizardState state) {
			try {
				StringBuilder template = new StringBuilder();
				template.append("protocol.name=" + state.protocolName + "\n");
				template.append("protocol.team=" + state.protocolTeam + "\n");
				template.append("template=cellprofiler\n");
				template.append("plate.folderpattern=" + guessPlateFolderPattern(state.selectedFolder) + "\n");
				template.append("welldata.path=" + makeRelative(state.selectedWellDataFile.getParent(), state.selectedFolder) + "\n");
				template.append("welldata.filepattern=" + state.selectedWellDataFile.getFileName().toString() + "\n");
				String idColumn = Arrays.stream(state.selectedWellDataHeaders).collect(Collectors.joining(","));
				if (!idColumn.isEmpty()) template.append("welldata.idcolumn=" + idColumn + "\n");
				
				if (state.includeSubWellData) {
					Path swPath = state.singleSubWellDataFile ? state.selectedSubWellDataFile.getParent() : state.subWellDataFolder;
					template.append("subwelldata.path=" + makeRelative(swPath, state.selectedFolder) + "\n");
					String swPattern = state.singleSubWellDataFile ? state.selectedSubWellDataFile.getFileName().toString() : state.subWellDataFilePattern;
					template.append("subwelldata.filepattern=" + swPattern + "\n");
					if (state.singleSubWellDataFile) {
						template.append("subwelldata.idcolumn=" + Arrays.stream(state.selectedSubWellDataHeaders).collect(Collectors.joining(",")) + "\n");
					}
				}
				
				for (int i=0; i<state.imageChannels.size(); i++) {
					ImageChannel ch = state.imageChannels.get(i);

					template.append("imagedata.channel." + (i+1) + ".name=" + ch.getName() + "\n");
					template.append("imagedata.channel." + (i+1) + ".path=" + makeRelative(state.selectedImageFolder, state.selectedFolder) + "\n");
					template.append("imagedata.channel." + (i+1) + ".filepattern=" + ch.getChannelConfig().get("pattern") + "\n");
					template.append("imagedata.channel." + (i+1) + ".color=" + ch.getColorMask() + "\n");

					GroupRole[] groupRoles = PatternConfig.deserializeRoles(ch.getChannelConfig().get("groupRoles"));
					String idGroup = PatternConfig.toIdGroupString(groupRoles);
					if (idGroup != null) template.append("imagedata.channel." + (i+1) + ".idgroup=" + idGroup + "\n");
					String fieldGroup = PatternConfig.toFieldGroupString(groupRoles);					
					if (fieldGroup != null) template.append("imagedata.channel." + (i+1) + ".fieldgroup=" + fieldGroup + "\n");

					if (ch.getType() == ImageChannel.CHANNEL_TYPE_RAW) {
						template.append("imagedata.channel." + (i+1) + ".contrast.min=" + ch.getLevelMin() + "\n");
						template.append("imagedata.channel." + (i+1) + ".contrast.max=" + ch.getLevelMax() + "\n");
						template.append("imagedata.channel." + (i+1) + ".depth=" + ch.getBitDepth() + "\n");
					} else {
						template.append("imagedata.channel." + (i+1) + ".type=Overlay\n");
					}

					if (fieldGroup != null) {
						template.append("imagedata.channel." + (i+1) + ".montage=true\n");
					}
				}

				if (state.wellDataHeaders != null) {
					for (int i=0; i<state.wellDataHeaders.length; i++) {
						template.append("wellfeature." + (i+1) + ".name=" + state.wellDataHeaders[i] + "\n");
						template.append("wellfeature." + (i+1) + ".numeric=true\n");
					}
				}
				
				if (state.subWellDataHeaders != null) {
					for (int i=0; i<state.subWellDataHeaders.length; i++) {
						template.append("subwellfeature." + (i+1) + ".name=" + state.subWellDataHeaders[i] + "\n");
						template.append("subwellfeature." + (i+1) + ".numeric=true\n");
					}
				}

				return ProtocolTemplateService.getInstance().executeTemplate(template.toString(), new NullProgressMonitor());
			} catch (Exception e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						"Failed to create protocol",
						"An error occurred while creating the protocol:\n" + e.getMessage());
				return null;
			}
		}
		
		private String guessPlateFolderPattern(Path selectedFolder) {
			String regex = "(\\d+.*)";
			String folderName = selectedFolder.getFileName().toString();
			if (Pattern.matches(regex, folderName)) return regex;
			return "(.*)";
		}
		
		private String makeRelative(Path path, Path base) {
			 String rel = base.relativize(path).toString();
			 if (!rel.isEmpty()) rel = "/" + rel;
			 return "." + rel;
		}
	}
}
