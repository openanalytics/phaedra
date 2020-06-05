package eu.openanalytics.phaedra.ui.columbus.protocolwizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.datacapture.columbus.ColumbusService;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens.Screen;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetUsers.User;
import eu.openanalytics.phaedra.datacapture.util.FeatureDefinition;
import eu.openanalytics.phaedra.link.importer.ImportService;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.link.importer.ImportUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateService;

public class ColumbusProtocolWizard extends BaseStatefulWizard {

	public ColumbusProtocolWizard() {
		setPreconfiguredState(new WizardState());
		setNeedsProgressMonitor(true);
	}
	
	@Override
	public void addPages() {
		addPage(new SelectScreenPage());
		addPage(new ImageChannelPage());
		addPage(new WellFeaturePage());
		addPage(new SubWellFeaturePage());
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
		String userName = (state.user == null) ? "columbus" : state.user.loginname;
		task.sourcePath = "Columbus/" + userName + "/" + state.screen.screenName;
		task.userName = SecurityService.getInstance().getCurrentUserName();
		ColumbusService.getInstance().setInstanceConfig(task.getParameters(), state.instanceId);
		
		// If user requested a new experiment, create it now.
		if (task.targetExperiment == null) {
			if (!checkFinish(ImportUtils.createExperiment(task, p, state.screen.screenName))) {
				return false;
			}
		}
		
		return checkFinish(
				ImportService.getInstance().startJob(task) );
	}
	
	private boolean checkFinish(final IStatus status) {
		if (status.getSeverity() == IStatus.ERROR) {
			MessageDialog.openError(getShell(), "Import", status.getMessage());
			return false;
		}
		return true;
	}
	
	
	public static class WizardState implements IWizardState {
		
		public String protocolName;
		public String protocolTeam;
		
		public String instanceId;
		public User user;
		public Screen screen;
		public ColumbusScreenAnalyzer analyzer = new ColumbusScreenAnalyzer();
		public boolean autoImport;
		
		public Map<String, Object> parameters = new HashMap<>();
		
		public List<ImageChannel> imageChannels = new ArrayList<>();
		public List<String> imageChannelPatterns = new ArrayList<>();
		public List<ImageData> imageChannelThumbs = new ArrayList<>();
		public List<FeatureDefinition> wellFeatures = new ArrayList<>();
		public List<FeatureDefinition> subwellFeatures = new ArrayList<>();
		
		public void analyze(IProgressMonitor monitor) throws IOException {
			analyzer.load(this, monitor);
		}
	}
	
	private static class ColumbusScreenAnalyzer {

		private final static String SCRIPT_NAME = "protocol/columbus.screen.analyzer.js";
		
		public void load(WizardState state, IProgressMonitor monitor) throws IOException {
			try {
				state.imageChannels.clear();
				state.imageChannelPatterns.clear();
				state.imageChannelThumbs.clear();
				state.wellFeatures.clear();
				state.subwellFeatures.clear();
				Map<String,Object> params = new HashMap<>();
				params.put("monitor", monitor);
				params.put("screen", state.screen);
				params.put("instanceId", state.instanceId);
				params.put("imageChannels", state.imageChannels);
				params.put("imageChannelPatterns", state.imageChannelPatterns);
				params.put("imageChannelThumbs", state.imageChannelThumbs);
				params.put("wellFeatures", state.wellFeatures);
				params.put("subwellFeatures", state.subwellFeatures);
				params.put("parameters", state.parameters);
				ScriptService.getInstance().getCatalog().run(SCRIPT_NAME, params);
			} catch (ScriptException e) {
				throw new IOException(e);
			}
		}
	}
	
	private static class ProtocolCreator {
		
		private final static String SCRIPT_NAME = "protocol/columbus.template.creator.js";
		
		public Protocol run(WizardState state) {
			List<ImageChannel> orderedChannels = state.imageChannels.stream()
					.filter(ch -> ch.getSequence() != -1)
					.sorted((c1,c2) -> c1.getSequence() - c2.getSequence())
					.collect(Collectors.toList());
			
			List<String> orderedPatterns = orderedChannels.stream()
					.map(ch -> {
						int index = 0;
						for (int i=0; i<state.imageChannels.size(); i++) {
							if (state.imageChannels.get(i) == ch) index = i;
						}
						return state.imageChannelPatterns.get(index);
					})
					.collect(Collectors.toList());
			
			try {
				StringBuilder template = new StringBuilder();
				Map<String,Object> params = new HashMap<>();
				params.put("template", template);
				params.put("protocolName", state.protocolName);
				params.put("protocolTeam", state.protocolTeam);
				params.put("screen", state.screen);
				params.put("imageChannels", orderedChannels.toArray());
				params.put("imageChannelPatterns", orderedPatterns.toArray());
				params.put("wellFeatures", state.wellFeatures.toArray());
				params.put("subwellFeatures", state.subwellFeatures.toArray());
				params.put("parameters", state.parameters);
				ScriptService.getInstance().getCatalog().run(SCRIPT_NAME, params);
				return ProtocolTemplateService.getInstance().executeTemplate(template.toString(), new NullProgressMonitor());
			} catch (Exception e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						"Failed to create protocol",
						"An error occurred while creating the protocol:\n" + e.getMessage());
				return null;
			}
		}
	}
}
