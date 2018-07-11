package eu.openanalytics.phaedra.ui.silo.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.ui.silo.util.SiloPasteDialog;
import eu.openanalytics.phaedra.ui.silo.util.SiloPasteSettings;

public class PasteSWItemsAdvanced extends PasteSWItems {

	private SiloPasteSettings settings;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		this.settings = new SiloPasteSettings();

		SiloPasteDialog dialog = new SiloPasteDialog(Display.getDefault().getActiveShell(), settings);

		if (dialog.open() != Window.OK) return null;

		return super.execute(event);
	}

	@Override
	protected boolean asNewDataset() {
		return settings.isNewGroup();
	}

	@Override
	protected List<SubWellItem> getItems(ISelection selection) {
		List<SubWellItem> swItems = super.getItems(selection);
		List<SubWellItem> filteredItems = new ArrayList<>();

		Random rnd = null;
		if (settings.getSubsetPct() != null) {
			rnd = new Random();
		}

		for (SubWellItem sw : swItems) {
			// Check if the Well matches the settings.
			if (settings.isValidWell(sw.getWell())) {
				// Take random selection from Wells if needed.
				if (rnd == null || rnd.nextDouble() < settings.getSubsetPct()) {
					filteredItems.add(sw);
				}
			}
		}

		return filteredItems;
	}

}