package eu.openanalytics.phaedra.ui.columbus.protocolwizard;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.ui.columbus.protocolwizard.ColumbusProtocolWizard.WizardState;
import eu.openanalytics.phaedra.ui.columbus.util.ImageChannelConfigItem;
import eu.openanalytics.phaedra.ui.columbus.util.ImageChannelOrderManager;

public class ImageChannelPage extends BaseStatefulWizardPage {

	private ImageChannelOrderManager orderMgr;
	
	private Group rawChannelContainer;
	private Group overlayChannelContainer;
	
	protected ImageChannelPage() {
		super("Image Channels");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);

		rawChannelContainer = new Group(container, SWT.NONE);
		rawChannelContainer.setText("Image Channels");
		GridDataFactory.fillDefaults().grab(true, true).applyTo(rawChannelContainer);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(rawChannelContainer);
		
		overlayChannelContainer = new Group(container, SWT.NONE);
		overlayChannelContainer.setText("Image Overlays");
		GridDataFactory.fillDefaults().grab(true, true).applyTo(overlayChannelContainer);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(overlayChannelContainer);
		
		orderMgr = new ImageChannelOrderManager();
		
		setTitle("Image Channels");
    	setDescription("Configure the image channels as they should be saved in the Protocol.");
    	setControl(container);
    	setPageComplete(true);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		WizardState s = (WizardState)state;
		orderMgr.reset();
		for (Control child: rawChannelContainer.getChildren()) child.dispose();
		for (Control child: overlayChannelContainer.getChildren()) child.dispose();
		for (int i = 0; i < s.imageChannels.size(); i++) {
			ImageChannel ch = s.imageChannels.get(i);
			ImageData thumb = (s.imageChannelThumbs.size() > i) ? s.imageChannelThumbs.get(i) : null;
			ImageChannelConfigItem item;
			
			if (ch.getType() == ImageChannel.CHANNEL_TYPE_RAW) item = new ImageChannelConfigItem(rawChannelContainer, ch, thumb, orderMgr);
			else item = new ImageChannelConfigItem(overlayChannelContainer, ch, thumb, orderMgr);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(item.getControl());
		}
		rawChannelContainer.layout();
		overlayChannelContainer.layout();
		
		// Resize, because the contents of this page are dynamic, and can be bigger than the previous page.
		getShell().setSize(getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	@Override
	public void collectState(IWizardState state) {
		// Nothing to collect.
	}

}
