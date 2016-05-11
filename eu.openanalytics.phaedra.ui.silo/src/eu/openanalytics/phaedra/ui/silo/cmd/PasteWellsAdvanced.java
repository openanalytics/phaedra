package eu.openanalytics.phaedra.ui.silo.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.silo.util.SiloPasteDialog;
import eu.openanalytics.phaedra.ui.silo.util.SiloPasteSettings;

public class PasteWellsAdvanced extends PasteWells {

	private SiloPasteSettings settings;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		this.settings = new SiloPasteSettings();

		SiloPasteDialog dialog = new SiloPasteDialog(Display.getDefault().getActiveShell(), settings);

		if (dialog.open() != Window.OK) return null;

		return super.execute(event);
	}

	@Override
	protected boolean asNewGroup() {
		return settings.isNewGroup();
	}

	@Override
	protected List<Well> getItems(ISelection selection) {
		List<Well> wells = super.getItems(selection);
		List<Well> filteredWells = new ArrayList<>();

		Random rnd = null;
		if (settings.getSubsetPct() != null) {
			rnd = new Random();
		}

		for (Well w : wells) {
			// Check if the Well matches the settings.
			if (settings.isValidWell(w)) {
				// Take random selection from Wells if needed.
				if (rnd == null || rnd.nextDouble() < settings.getSubsetPct()) {
					filteredWells.add(w);
				}
			}
		}

		return filteredWells;
	}


}