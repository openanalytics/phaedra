package eu.openanalytics.phaedra.ui.plate.wellimage;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.protocol.ImageSettingsService;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageSettingsPanel;

public class ImageSettingsView extends ViewPart {

	private ImageSettingsPanel panel;

	private IUIEventListener settingsListener;
	private ISelectionListener selectionListener;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().margins(2,2).applyTo(parent);

		panel = new ImageSettingsPanel(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(panel);

		// Get the preferred size of the settings panel.
		panel.layout();
		Point idealSize = panel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		panel.setSize(idealSize);

		// Setting handling
		settingsListener = event -> {
			if (event.type == EventType.ImageSettingsChanged) {
				panel.load(ImageSettingsService.getInstance().getCurrentSettings(null));
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(settingsListener);

		// Selection handling
		selectionListener = (part, selection) -> loadWell(SelectionUtils.getFirstObject(selection, Well.class));
		getSite().getPage().addSelectionListener(selectionListener);
		getViewSite().getActionBars().getToolBarManager().add(panel.getModeButton());

		// Obtain initial state.
		panel.load(ImageSettingsService.getInstance().getCurrentSettings(null));

		SelectionUtils.triggerActiveEditorSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewImageSettings");
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ProtocolUIService.getInstance().removeUIEventListener(settingsListener);
		super.dispose();
	}

	@Override
	public void setFocus() {
		panel.setFocus();
	}

	public void loadWell(Well well) {
		if (well != null) panel.load(well);
	}
}
