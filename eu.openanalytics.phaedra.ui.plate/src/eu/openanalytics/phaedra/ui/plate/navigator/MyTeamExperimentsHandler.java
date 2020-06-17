package eu.openanalytics.phaedra.ui.plate.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.editor.IDynamicVOCollector;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.icons.IconRegistry;
import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.ui.plate.browser.ExperimentBrowser;

public class MyTeamExperimentsHandler extends BaseElementHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().equals("team.experiments"));
	}

	@Override
	public void handleDoubleClick(IElement element) {
		EditorFactory.getInstance().openEditor(new VOEditorInput(new TeamExperimentsCollector()), ExperimentBrowser.class.getName());
	}

	public static class TeamExperimentsCollector implements IDynamicVOCollector {

		@Override
		public String getEditorName() {
			return "Team Experiments";
		}

		@Override
		public String getVOClassName() {
			return Experiment.class.getName();
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return IconRegistry.getInstance().getDefaultImageDescriptorFor(Experiment.class);
		}

		@Override
		public List<IValueObject> collect(Map<String, Object> filter) {
			boolean excludeClosed= Boolean.TRUE.equals(filter.get("Experiment.excludeClosed"));
			return new ArrayList<>(PlateService.getInstance().getExperiments((String)null, excludeClosed));
		}
		
	}
}